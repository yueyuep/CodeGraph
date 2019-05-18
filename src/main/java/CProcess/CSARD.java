package CProcess;

import GraphProcess.Graph2Json;
import com.google.common.graph.MutableNetwork;
import org.eclipse.cdt.core.dom.ast.IASTCompoundStatement;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.internal.core.dom.parser.cpp.*;

import java.io.File;
import java.lang.reflect.MalformedParameterizedTypeException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CSARD {
    public static void main(String[] args) {
//        getSARDGraphs("cf", "cfg");
        getSARDGraphs("CWE-119", "CWE-119G");
    }

    private static void getSARDGraphs(String sourceDir, String graphDir) {
//        String source = "../CodeGraph/" + sourceDir + "/";
//        String des = "../CodeGraph/" + graphDir + "/";
        String source = "../benchmark/" + sourceDir + "/";
        String des = "../benchmark/" + graphDir + "/";
        String desT = des + "trueg/";
        String desF = des + "falseg/";
        mkdirIfNotExists(des);
        mkdirIfNotExists(desT);
        mkdirIfNotExists(desF);
        for (File subDir : new File(source).listFiles()) {
            File[] cweFiles = subDir.listFiles();
            if (cweFiles.length == 1) {
//                continue;
                File cwe = cweFiles[0];
                if (cwe.getName().startsWith("CWE")) {
                    genSaveSardSingelGraph(cwe.getPath(), cwe.getName(), desT, desF);
                }
            }
            if (cweFiles.length > 1) {
                if (cweFiles[0].getName().startsWith("CWE")) {
                    genSaveSardMultiGraph(cweFiles, desT, desF);
                }
            }
        }
    }

    public static void genSaveSardMultiGraph(File[] files, String desDirT, String desDirF) {
        Map<File, BuildGraphC> fileGraphMap = new HashMap<>();
        for (File file : files) {
            if (file.getName().endsWith(".h")) {
                continue;
            }
            BuildGraphC bc = BuildGraphC.newFromFile(file.getPath());
            if (bc == null) {
                System.out.println("Null: " + file.getPath());
            } else {
                fileGraphMap.put(file, bc);
            }
        }
        File aFile = null;
        for (File file : fileGraphMap.keySet()) {
            if (file.getName().endsWith("a.cpp") || file.getName().endsWith("a.c")) {
                aFile = file;
                break;
            }
        }
        if (aFile == null) {
            return;
        }
        BuildGraphC aBC = fileGraphMap.get(aFile);
        int falseG = 0;
        int trueG = 0;
        for (IASTFunctionDefinition dec : aBC.getFunctionDefinitions()) {
            CPPASTFunctionDeclarator fdTor = (CPPASTFunctionDeclarator) dec.getDeclarator();
            String funcName = fdTor.getName().toString();
            if (funcName.endsWith("bad")) {
                for (File file : fileGraphMap.keySet()) {
                    fileGraphMap.put(file, BuildGraphC.newFromFile(file.getPath()));
                }
                aBC = fileGraphMap.get(aFile);
                trueG = getGraphCrossMultiFile(aBC, dec, fileGraphMap, desDirT, aFile, trueG);
//                System.out.println(trueG);
            }
            if (funcName.endsWith("good")) {
                for (CPPASTFunctionCallExpression fc : aBC.findAll(dec, CPPASTFunctionCallExpression.class)) {
                    String functionCallName = aBC.findAll(fc.getFunctionNameExpression(), IASTName.class).get(0).toString();
                    for (IASTFunctionDefinition calledFuncDef : aBC.getFunctionDefinitions()) {
                        CPPASTFunctionDeclarator calledFdTor = (CPPASTFunctionDeclarator) calledFuncDef.getDeclarator();
                        String calledName = calledFdTor.getName().toString();
                        if (functionCallName.equals(calledName)) {
                            for (File file : fileGraphMap.keySet()) {
                                fileGraphMap.put(file, BuildGraphC.newFromFile(file.getPath()));
                            }
                            aBC = fileGraphMap.get(aFile);
                            falseG = getGraphCrossMultiFile(aBC, calledFuncDef, fileGraphMap, desDirF, aFile, falseG);
//                            System.out.println(falseG);
                        }
                    }
                }
            }
        }
    }

    public static int getGraphCrossMultiFile(BuildGraphC aBC, IASTFunctionDefinition funcDef,
                                             Map<File, BuildGraphC> fileGraphMap, String desDir, File aFile, int graphNum) {
        aBC.initNetwork();
        Map<File, List<IASTFunctionDefinition>> visitedFileFunc = new HashMap<>();
        getFuncDefGraphInMultiFiles(aBC, funcDef, fileGraphMap, aFile, visitedFileFunc);
        MutableNetwork<Object, String> network = aBC.getNetwork();
        if (!network.edges().isEmpty()) {
            graphNum++;
            Graph2Json graph2Json = Graph2Json.newInstance(network);
            graph2Json.saveToJson(desDir + aFile.getName() + "_" + graphNum + "_.txt");
            System.out.println(desDir + aFile.getName() + "_" + graphNum + "_.txt");
        }
        return graphNum;
    }

    private static void getFuncDefGraphInMultiFiles(BuildGraphC bc, IASTFunctionDefinition funcDefinition,
                                                   Map<File, BuildGraphC> fileGraphMap, File file,
                                                   Map<File, List<IASTFunctionDefinition>> visitedFileFunc) {
        bc.buildGraphWithoutInit(funcDefinition);
        // 将已经构建图的方法声明保存到Map，后面进行检查，以免重复构图。
        if (!visitedFileFunc.containsKey(file)) {
            visitedFileFunc.put(file, new ArrayList<>());
        }
        for (Object node : bc.getNetwork().nodes()) {
            if (node instanceof IASTFunctionDefinition) {
                visitedFileFunc.get(file).add((IASTFunctionDefinition) node);
            }
        }
        List<CPPASTFunctionCallExpression> visitedFuncCall = new ArrayList<>();
        // C++中类的名字与其他文件的名字是否一样，若一样则检查方法调用与FieldReference是否在那个文件中声明
        for (CPPASTName className : bc.findAll(funcDefinition.getBody(), CPPASTName.class)) {
            if (className.toString().endsWith("base")) {
                continue;
            }
            for (File otherFile : fileGraphMap.keySet()) {
                if (!otherFile.getName().equals(file.getName()) && otherFile.getName().contains(className.toString())) {
                    if (!fileGraphMap.containsKey(otherFile)) {
                        System.out.println("Not contain the file: " + otherFile.getName());
                        continue;
                    }
                    BuildGraphC otherBC = fileGraphMap.get(otherFile);
                    for (CPPASTFunctionCallExpression fc : bc.findAll(funcDefinition, CPPASTFunctionCallExpression.class)) {
                        if (visitedFuncCall.contains(fc)) {
                            continue;
                        }
                        String functionCallName;
                        if (fc.getFunctionNameExpression() instanceof CPPASTFieldReference) {
                            functionCallName = ((CPPASTFieldReference) fc.getFunctionNameExpression()).getFieldName().toString();
                        } else {
                            functionCallName = bc.findAll(fc.getFunctionNameExpression(), IASTName.class).get(0).toString();
                        }
                        connectFuncCallToOtherFile(bc, fileGraphMap, visitedFileFunc, visitedFuncCall, fc, functionCallName, otherFile, otherBC);
                    }
                }
            }
        }
        // C中方法的名字与其他文件的方法名字是否一样
        for (CPPASTFunctionCallExpression fc : bc.findAll(funcDefinition, CPPASTFunctionCallExpression.class)) {
            if (visitedFuncCall.contains(fc)) {
                continue;
            }
            String functionCallName = bc.findAll(fc.getFunctionNameExpression(), IASTName.class).get(0).toString();
            for (File otherFile : fileGraphMap.keySet()) {
                if (!fileGraphMap.containsKey(otherFile)) {
                    System.out.println("Not contain the file: " + otherFile.getName());
                    continue;
                }
                BuildGraphC otherBC = fileGraphMap.get(otherFile);
                connectFuncCallToOtherFile(bc, fileGraphMap, visitedFileFunc, visitedFuncCall, fc, functionCallName, otherFile, otherBC);
            }
        }
    }

    private static void connectFuncCallToOtherFile(BuildGraphC bc, Map<File, BuildGraphC> fileGraphMap, Map<File,
            List<IASTFunctionDefinition>> visitedFileFunc, List<CPPASTFunctionCallExpression> visitedFuncCall,
                                                   CPPASTFunctionCallExpression fc, String functionCallName,
                                                   File otherFile, BuildGraphC otherBC) {
        for (IASTFunctionDefinition calledFuncDef : otherBC.getFunctionDefinitions()) {
            CPPASTFunctionDeclarator calledFdTor = (CPPASTFunctionDeclarator) calledFuncDef.getDeclarator();
            if ((functionCallName.equals(calledFdTor.getName().toString()) || calledFdTor.getName().toString().contains(functionCallName))
                    && fc.getArguments().length == calledFdTor.getParameters().length) {
                visitedFuncCall.add(fc);
                // 添加方法调用边
                bc.addFormalArgs(fc, calledFdTor);
                bc.addMethodCall(fc, calledFuncDef);
                // 在已经构造的图的基础上，对被调方法的声明构图
                if (visitedFileFunc.containsKey(otherFile) && visitedFileFunc.get(otherFile).contains(calledFuncDef)) {
                    continue;
                }
                otherBC.initNetwork();
                otherBC.setNetwork(bc.getNetwork());
                otherBC.setEdgeNumber(bc.getEdgeNumber());
                getFuncDefGraphInMultiFiles(otherBC, calledFuncDef, fileGraphMap, otherFile, visitedFileFunc);
                bc.setNetwork(otherBC.getNetwork());
                bc.setEdgeNumber(otherBC.getEdgeNumber());
            }
        }
    }

    public static void genSaveSardSingelGraph(String filePath, String fileName, String desDirT, String desDirF) {
        BuildGraphC bc = BuildGraphC.newFromFile(filePath);
        if (bc == null) {
            System.out.println("Null: " + filePath);
            return;
        }
        int falseG = 0;
        int trueG = 0;
        for (IASTFunctionDefinition dec : bc.getFunctionDefinitions()) {
            CPPASTFunctionDeclarator fdTor = (CPPASTFunctionDeclarator) dec.getDeclarator();
            String funcName = fdTor.getName().toString();
            if (funcName.endsWith("bad")) { // bad()方法只有一个
                trueG = getFuncDefGraph(bc, dec, fileName, desDirT, trueG);
            }
            bc.initNetwork();
            if (funcName.endsWith("good")) { // good()方法里有多个goodXXX()方法调用，每一个方法调用都是一个独立的图
                for (CPPASTFunctionCallExpression fc : bc.findAll(dec, CPPASTFunctionCallExpression.class)) {
                    String functionCallName = bc.findAll(fc.getFunctionNameExpression(), IASTName.class).get(0).toString();
                    for (IASTFunctionDefinition calledFuncDef : bc.getFunctionDefinitions()) {
                        CPPASTFunctionDeclarator calledFdTor = (CPPASTFunctionDeclarator) calledFuncDef.getDeclarator();
                        String calledName = calledFdTor.getName().toString();
                        if (functionCallName.equals(calledName)) {
                            falseG = getFuncDefGraph(bc, calledFuncDef, fileName, desDirF, falseG);
                        }
                    }
                }
            }
        }
    }

    public static int getFuncDefGraph(BuildGraphC bc, IASTFunctionDefinition funcDefinition, String fileName, String desDirName, int graphNum) {
        bc.buildGraph(funcDefinition);
        MutableNetwork<Object, String> network = bc.getNetwork();
        if (!network.edges().isEmpty()) {
            graphNum++;
            Graph2Json graph2Json = Graph2Json.newInstance(network);
            graph2Json.saveToJson(desDirName + fileName + "_" + graphNum + "_.txt");
            System.out.println(desDirName + fileName + "_" + graphNum + "_.txt");
        }
        return graphNum;
    }

    public static void mkdirIfNotExists(String dirPath) {
        File dir = new File(dirPath);
        if (!dir.exists()) {
            dir.mkdir();
        }
    }
}
