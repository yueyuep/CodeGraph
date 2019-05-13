package CProcess;

import GraphProcess.Graph2Json;
import com.google.common.graph.MutableNetwork;
import org.eclipse.cdt.core.dom.ast.IASTCompoundStatement;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTFunctionCallExpression;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTFunctionDeclarator;

import java.io.File;

public class CSARD {
    public static void main(String[] args) {
        getSARDGraphs("CWE-399-SARD", "CWE-399-SARDG");
    }

    private static void getSARDGraphs(String sourceDir, String graphDir) {
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
                File cwe = cweFiles[0];
                if (cwe.getName().startsWith("CWE")) {
                    genSaveSardGraph(cwe.getPath(), cwe.getName(), desT, desF);
                }
            }
        }
    }

    public static void genSaveSardGraph(String filePath, String fileName, String desDirT, String desDirF) {
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
            if (funcName.equals("bad") || funcName.endsWith("bad")) {
                trueG = getFuncDefGraph(fileName, desDirT, bc, trueG, dec);
            }
            bc.initNetwork();
            if (funcName.equals("good") || funcName.endsWith("good")) {
                for (CPPASTFunctionCallExpression fc : bc.findAll(dec, CPPASTFunctionCallExpression.class)) {
                    String functionCallName = bc.findAll(fc.getFunctionNameExpression(), IASTName.class).get(0).toString();
                    for (IASTFunctionDefinition calledFuncDef : bc.getFunctionDefinitions()) {
                        CPPASTFunctionDeclarator calledFdTor = (CPPASTFunctionDeclarator) calledFuncDef.getDeclarator();
                        String calledName = calledFdTor.getName().toString();
                        if (functionCallName.equals(calledName)) {
                            falseG = getFuncDefGraph(fileName, desDirF, bc, falseG, calledFuncDef);
                        }
                    }
                }
            }
        }
    }

    public static int getFuncDefGraph(String fileName, String desDirName, BuildGraphC bc, int graphNum, IASTFunctionDefinition calledFuncDef) {
        bc.buildGraph(calledFuncDef);
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
