package owaspbench;

import GraphProcess.*;
import com.github.javaparser.JavaParser;
import com.github.javaparser.JavaToken;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.comments.Comment;
import com.github.javaparser.ast.expr.CharLiteralExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.google.common.graph.MutableNetwork;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @ Author     ：wxkong
 */
public class ExtractSubGraphs {
    String mSrcFileName;
    String mSaveFileName;

    private ExtractSubGraphs(String srcFileName, String saveFileName) {
        mSrcFileName = srcFileName;
        mSaveFileName = saveFileName;
    }

    public ExtractSubGraphs newInstance(String srcFileName, String saveFileName) {
        return new ExtractSubGraphs(srcFileName, saveFileName);
    }

    public static void ExtractSQLI(String filePath, String graphPath) {
        AST2Graph ast2Graph = AST2Graph.newInstance(filePath);
        if (ast2Graph == null) {
            System.out.println(filePath + ": null");
            return;
        }
        List<MethodCallExpr> relateMethodCalls = new ArrayList<>();
        List<MethodDeclaration> methodDecls = new ArrayList<>();
        for (MethodCallExpr mc : ast2Graph.getCompilationUnit().findAll(MethodCallExpr.class)) {
            if (SQLI.ExecuteMethods.contains(mc.getNameAsString())) {
                relateMethodCalls.add(mc);
                methodDecls.add(ast2Graph.findMethodDeclarationContains(mc));
            }
        }
        for (int i = 0; i < relateMethodCalls.size(); i++) {
            MethodCallExpr callExpr = relateMethodCalls.get(i);
            MethodDeclaration decl = methodDecls.get(i);
            if (decl == null) {
                continue;
            }
            ast2Graph.initNetwork();
            ast2Graph.constructNetwork(decl);
            MutableNetwork<Object, String> network = ast2Graph.getNetwork();
            List<Node> vars = new ArrayList<>();
            callExpr.getArguments().forEach(argument -> vars.addAll(new ArrayList<>(argument.findAll(NameExpr.class))));
            callExpr.getScope().ifPresent(scope -> vars.addAll(new ArrayList<>(scope.findAll(NameExpr.class))));
            List<RangeNode> rangeVars = new ArrayList<>();
            vars.forEach(var -> rangeVars.add(GenerateGraph.findNodeInNetwork(network, var)));
            Set<RangeNode> dataFlowNodes = new HashSet<>();
            for (RangeNode varNode : rangeVars) {
                if (varNode == null || dataFlowNodes.contains(varNode)) {
                    continue;
                }
                dataFlowNodes.addAll(ast2Graph.getRelatedDataFlowNodes(varNode, dataFlowNodes, new HashSet<RangeNode>()));
            }
            RemoveNode removeNode = RemoveNode.newInstance(filePath);
            if (removeNode == null) {
                System.out.println(filePath);
                return;
            }
//            ast2Graph.initNetworkWithoutRangeNode();
            removeNode.setRelatedNodes(dataFlowNodes);
            removeNode.setCallExpr(callExpr);
            removeNode.initNetwork();
            removeNode.constructNetwork(decl);
            MutableNetwork<Object, String> graph = removeNode.getNetwork();
            if (!graph.edges().isEmpty()) {
                Graph2Json graph2Json = Graph2Json.newInstance(graph);
                String save = graphPath + ".txt";
                if (i != 0) {
                    save = graphPath + "_" + i + ".txt";
                }
                graph2Json.saveToJson(save);
                System.out.println(save);
            }
        }
    }

    private static void removeNotContain(MutableNetwork<Object, String> network, Set<RangeNode> nodes) {
        Set<Object> toRemoved = new HashSet<>();
        for (Object node : network.nodes()) {
            if (node instanceof RangeNode) {
                Node parent = ((RangeNode) node).getNode();
                boolean remove = true;
                for (RangeNode rn : nodes) {
                    Node child = rn.getNode();
                    if (node.equals(rn) || parent.findAll(Node.class).contains(child)) {
                        remove = false;
                        break;
                    }
                }
                if (remove) {
                    toRemoved.add(node);
                }
            }
        }
        removeNodes(network, toRemoved);
    }

    private static void removeNodes(MutableNetwork<Object, String> network, Set<Object> toRemoved) {
        for (Object node : toRemoved) {
            network.removeNode(node);
        }
        Set<Object> remove = new HashSet<>();
        for (Object node : network.nodes()) {
            if (network.incidentEdges(node).isEmpty()) {
                remove.add(node);
            }
        }
        for (Object node : remove) {
            network.removeNode(node);
        }
    }

    public static void ExtractOwaspBackup(String filePath, String graphPath) {
        AST2Graph ast2Graph = AST2Graph.newInstance(filePath);
        if (ast2Graph == null) {
            System.out.println(filePath);
            return;
        }
        List<MethodDeclaration> methodPosts = ast2Graph.getCompilationUnit().findAll(MethodDeclaration.class).stream()
                .filter(methodDeclaration -> methodDeclaration.getNameAsString().equals("doPost"))
                .collect(Collectors.toList());
        if (!methodPosts.isEmpty()) {
            MethodDeclaration methodPost = methodPosts.get(0);
            ast2Graph.initNetwork();
            ast2Graph.constructNetwork(methodPost);
//            ast2Graph.renameNetworkVar();
            MutableNetwork<Object, String> network = ast2Graph.getNetwork();
            if (!network.edges().isEmpty()) {
                Graph2Json graph2Json = Graph2Json.newInstance(network);
                graph2Json.saveToJson(graphPath + ".txt");
                System.out.println(graphPath + ".txt");
            }
        }
    }

    public static void getVulDeeResult(String filePath, String graphPath) {
        CompilationUnit cuOrigin;
        try {
            cuOrigin = JavaParser.parse(new FileInputStream(filePath));
        } catch (Exception e) {
            System.out.println(filePath + " error\n" + e);
            return;
        }
        List<String> comments = new ArrayList<>();
        for (Comment comment : cuOrigin.getAllContainedComments()) {
            comments.add(comment.toString());
        }
        processStringCharLiteral(cuOrigin);
        CompilationUnit cu = JavaParser.parse(cuOrigin.toString());
        StringBuilder sb = new StringBuilder();
//        List<String> methodCalls = new ArrayList<>();
//        cu.findAll(MethodCallExpr.class).forEach((MethodCallExpr mc) ->
//                methodCalls.add(mc.getNameAsString()));
//        for (String methodName : methodCalls) {
//            getStringFromMethod(comments, cu, sb, methodName);
//        }
        getStringFromMethod(comments, cu, sb, "doPost");
        String codeText = replaceBlank(sb.toString()).trim();
        Util.saveToFile(codeText, graphPath);
    }

    public static StringBuilder getStringFromMethod(List<String> comments, CompilationUnit cu, StringBuilder sb, String methodName) {
        List<MethodDeclaration> methodPosts = cu.findAll(MethodDeclaration.class).stream()
                .filter(methodDeclaration -> methodDeclaration.getNameAsString().equals(methodName))
                .collect(Collectors.toList());
        if (!methodPosts.isEmpty()) {
            MethodDeclaration postDecl = methodPosts.get(0);
            List<JavaToken> tokens = tokensOfNode(postDecl);
            tokens.forEach(javaToken -> {
                if (javaToken.getText().length() != 0 && !comments.contains(javaToken.getText())) {
                    sb.append(javaToken.getText().trim()).append(" ");
                }
            });
        }
        return sb;
    }

    public static String replaceBlank(String str){
        String dest = null;
        if(str == null){
            return dest;
        }else{
            Pattern p = Pattern.compile("\\s+|\t|\r|\n");
            Matcher m = p.matcher(str);
            dest = m.replaceAll(" ");
            return dest;
        }
    }

    public static void processStringCharLiteral(Node node) {
        for (StringLiteralExpr s : node.findAll(StringLiteralExpr.class)) {
            if (s.asString().isEmpty()) {
                s.setString("Empty String");
                s.setValue("Empty String");
            } else {
                s.setString("Not Empty String");
                s.setValue("Not Empty String");
            }
        }
        for (CharLiteralExpr c : node.findAll(CharLiteralExpr.class)) {
            c.setChar('c');
            c.setValue("C");
        }
    }

    public static ArrayList<JavaToken> tokensOfNode(Node node){
        ArrayList<JavaToken> tokens = new ArrayList<>();
        if (!node.getTokenRange().isPresent()) {
            return tokens;
        }
        JavaToken beginToken = node.getTokenRange().get().getBegin();
        JavaToken endToken = node.getTokenRange().get().getEnd();
        while(true){
            tokens.add(beginToken);
            if (beginToken.getNextToken().isPresent()) {
                beginToken = beginToken.getNextToken().get();
            } else {
                break;
            }
        }
//        tokens.add(endToken);
        return tokens;
    }


    public static void main(String[] args) {
        getVulDeeResult("data/BenchmarkTest21027.java", "");
//        ExtractSubGraphs.ExtractSQLI("data/BenchmarkTest21027.java", "data/BenchmarkTest21027.java.txt");
    }
}
