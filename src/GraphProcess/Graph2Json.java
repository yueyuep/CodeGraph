package GraphProcess;

import Util.SplitString;
import com.github.javaparser.ast.ArrayCreationLevel;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.CatchClause;
import com.github.javaparser.ast.stmt.ExplicitConstructorInvocationStmt;
import com.github.javaparser.ast.type.*;
import com.google.common.graph.MutableNetwork;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import owaspbench.VecGenerator;

import java.util.*;
import java.util.stream.Collectors;

public class Graph2Json {
    private MutableNetwork mNetwork;
    @Expose
    @SerializedName(value = "n_num")
    private int mNodeNumber;
    @Expose
    @SerializedName(value = "succs")
    private List<List<Integer>> mSuccessors = new ArrayList<>();
    @Expose
    @SerializedName(value = "featureString")
    private List<String> mFeatures = new ArrayList<>();
    @Expose
    @SerializedName(value = "featureDims")
    private List<List<Integer>> mFeatureDims = new ArrayList<>();

    private StringBuilder mStringBuilder = new StringBuilder();
    private VecGenerator mVecGenerator;

    private Graph2Json() {
    }

    public static Graph2Json newInstance(MutableNetwork mutableNetwork) {
        Graph2Json graph2Json = new Graph2Json();
        graph2Json.mNetwork = mutableNetwork;
        graph2Json.mVecGenerator = new VecGenerator(mutableNetwork);
        graph2Json.initSuccessors();
//        graph2Json.saveToJson();
        return graph2Json;
    }

    public void saveToJson() {
        String fileName = "data/000Test.txt";
        Util.saveToJsonFile(this, fileName);
    }

    public void saveToJson(String fileName) {
        Util.saveToJsonFile(this, fileName);
    }

    private void addFeature(Object node) {
        // 针对每个节点，保存Feature，目前是String格式，应改为Attributes的向量格式
        // 判断每个节点，如出入度边、节点是否是控制节点等，得到属性向量[0,1,0,2]
        if (node instanceof RangeNode) {
//            mFeatures.add(SplitString.splitUntilUpperCase(Util.getClassLastName(((RangeNode) node).getNode())));
            mFeatures.add(travelNode(((RangeNode) node).getNode()));
        } else if (node instanceof String) {
            mFeatures.add(node.toString());
        } else {
            mFeatures.add(node.toString());
        }
        mFeatureDims.add(mVecGenerator.getVecOfNode(node));
    }

    private void initSuccessors() {
        Map<Object, Integer> nodeMap = new HashMap<>();
        int nodeIndex = 0;
        for (Object node : mNetwork.nodes()) {
            nodeMap.put(node, nodeIndex);
            nodeIndex++;
        }
        mNodeNumber = nodeIndex;
        for (Object node : mNetwork.nodes()) {
            mSuccessors.add(
                    (List<Integer>) mNetwork.adjacentNodes(node).stream()
                            .map(o -> nodeMap.get(o))
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList())
            );
            addFeature(node);
        }
    }

    public List<List<Integer>> getSuccessors() {
        return mSuccessors;
    }

    public void setSuccessors(List<List<Integer>> successors) {
        mSuccessors = successors;
    }

    public List<String> getFeatures() {
        return mFeatures;
    }

    public void setFeatures(List<String> features) {
        mFeatures = features;
    }

    public <T extends Node> String travelNode(T node) {
        String nodeClassPackage = node.getClass().toString();
        String[] nodeClassPackageSplit = node.getClass().toString().split("\\.");
        String nodeClass = nodeClassPackageSplit[nodeClassPackageSplit.length - 1];
        node.removeComment();

        mStringBuilder.setLength(0);
        addStringToBuilder(SplitString.splitUntilUpperCase(nodeClass));

        if (isContain(nodeClassPackage, "Comment")) {
        } else if (isContain(nodeClassPackage, new String[] {"VoidType","UnknownType"})) {
        } else if (isContain(nodeClassPackage, "WildcardType")) {
            WildcardType wildcardType = (WildcardType) node;
            wildcardType.getSuperType().ifPresent(c -> {
                addStringToBuilder("SuperType");
            });
            wildcardType.getExtendedType().ifPresent(c -> {
                addStringToBuilder("ExtendedType");
            });
        } else if (isContain(nodeClassPackage, "UnionType")) {
            UnionType unionType = (UnionType) node;
            
        } else if (isContain(nodeClassPackage, "IntersectionType")) {
            IntersectionType intersectionType = (IntersectionType) node;
            
        } else if (isContain(nodeClassPackage, "ArrayType")) {
            ArrayType arrayType = (ArrayType) node;
            
            addStringToBuilder(String.valueOf(arrayType.getArrayLevel()));
        } else if (isContain(nodeClassPackage, "Annotation")) {
            addStringToBuilder(node);
        } else if (isContain(nodeClassPackage, "InitializerDeclaration")) {
            InitializerDeclaration initializerDeclaration = (InitializerDeclaration) node;

        } else if (isContain(nodeClassPackage, "AnnotationMemberDeclaration")) {
            AnnotationMemberDeclaration annotationMemberDeclaration = (AnnotationMemberDeclaration) node;
            
            annotationMemberDeclaration.getDefaultValue().ifPresent((c) -> addStringToBuilder("default " + c));
        } else if (isContain(nodeClassPackage, "AnnotationDeclaration")) {
            AnnotationDeclaration annotationDeclaration = (AnnotationDeclaration) node;
            addStringToBuilder(annotationDeclaration.getName());
        } else if (isContain(nodeClassPackage, "FieldDeclaration")) {
            FieldDeclaration fieldDeclaration = (FieldDeclaration) node;
        } else if (isContain(nodeClassPackage, "ClassOrInterfaceDeclaration")) {
            ClassOrInterfaceDeclaration classOrInterfaceDeclaration = (ClassOrInterfaceDeclaration) node;
            addStringToBuilder(classOrInterfaceDeclaration.getName());
        } else if (isContain(nodeClassPackage, "EnumDeclaration")) {
            EnumDeclaration enumDeclaration = (EnumDeclaration) node;
            addStringToBuilder(enumDeclaration.getName());
        } else if (isContain(nodeClassPackage, "EnumConstantDeclaration")) {
            EnumConstantDeclaration enumConstantDeclaration = (EnumConstantDeclaration) node;
            addStringToBuilder(enumConstantDeclaration.getName());
        } else if (isContain(nodeClassPackage, "MethodDeclaration")) {
            MethodDeclaration methodDeclaration = (MethodDeclaration) node;
            processThrows(methodDeclaration.getThrownExceptions());
        } else if (isContain(nodeClassPackage, "ConstructorDeclaration")) {
            ConstructorDeclaration constructorDeclaration = (ConstructorDeclaration) node;
            processThrows(constructorDeclaration.getThrownExceptions());
        } else if (isContain(nodeClassPackage, "ExplicitConstructorInvocationStmt")) {
            ExplicitConstructorInvocationStmt explicitConstructorInvocationStmt = (ExplicitConstructorInvocationStmt) node;
        } else if (isContain(nodeClassPackage, new String[]{"BreakStmt", "ContinueStmt"})) {
        } else if (isContain(nodeClassPackage, "ExpressionStmt")) {
        } else if (isContain(nodeClassPackage, "stmt")) {
        } else if (isContain(nodeClassPackage, "TypeParameter")) {
            TypeParameter parameter = (TypeParameter) node;
            if (parameter.getElementType().isUnknownType()){
                addStringToBuilder("UnknownType");
                addStringToBuilder(parameter.getNameAsString());
            } else {
                addStringToBuilder(parameter.getNameAsString());
            }
        } else if (isContain(nodeClassPackage, "Parameter")) {
            Parameter parameter = (Parameter) node;
            if (parameter.getType().isUnknownType()){
                addStringToBuilder(SplitString.splitUntilUpperCase("UnknownType"));
                addStringToBuilder(parameter.getNameAsString());
            } else {
                addStringToBuilder(parameter.getNameAsString());
            }
        } else if (isContain(nodeClassPackage, "ClassOrInterfaceType")) {
            ClassOrInterfaceType classOrInterfaceType = (ClassOrInterfaceType) node;
            // TODO: If need normalization
            addStringToBuilder(classOrInterfaceType.getName());
        } else if (isContain(nodeClassPackage, "CatchClause")) {
            CatchClause catchClause = (CatchClause) node;
            
        } else if (isContain(nodeClassPackage, "VariableDeclarationExpr")) {
            
            VariableDeclarationExpr variableDeclarationExpr = (VariableDeclarationExpr) node;
            if (!variableDeclarationExpr.getModifiers().isEmpty()) {
                addStringToBuilder(variableDeclarationExpr.getModifiers().toString());
            }
        }else if (isContain(nodeClassPackage, "VariableDeclarator")) {
            
            VariableDeclarator variableDeclarator = (VariableDeclarator) node;
            addStringToBuilder(variableDeclarator.getType());
            // TODO: If need normalization
            addStringToBuilder(variableDeclarator.getNameAsString());
            variableDeclarator.getInitializer().ifPresent((consumer) -> {
                addStringToBuilder("=");
            });
        } else if (isContain(nodeClassPackage, new String[]{"SimpleName","NameExpr","Name"})) {
            // TODO: If need normalization
            addStringToBuilder(node);
        } else if (isContain(nodeClassPackage, "Binary")) {
            BinaryExpr binaryExpr = (BinaryExpr) node;
            
            addStringToBuilder(binaryExpr.getOperator().toString());
        } else if (isContain(nodeClassPackage, "Unary")) {
            UnaryExpr unaryExpr = (UnaryExpr) node;
            
            addStringToBuilder(unaryExpr.getOperator().asString());
        }else if (isContain(nodeClassPackage, "CastExpr")) {
            CastExpr castExpr = (CastExpr) node;
            
        }else if (isContain(nodeClassPackage, "ClassExpr")) {
            
            addStringToBuilder(node);
        }else if (isContain(nodeClassPackage, "MethodCallExpr")) {
            MethodCallExpr methodCallExpr = (MethodCallExpr) node;
            
        }else if (isContain(nodeClassPackage, "MethodReferenceExpr")) {
            MethodReferenceExpr methodReferenceExpr = (MethodReferenceExpr) node;
            
            addStringToBuilder(methodReferenceExpr.getIdentifier());
        }else if (isContain(nodeClassPackage, "TypeExpr")) {
            
            addStringToBuilder(node);
        }else if (isContain(nodeClassPackage, "LiteralExpr")) {
            processLiteralExpr(node);
        }else if (isContain(nodeClassPackage, "AssignExpr")) {
            AssignExpr assignExpr = (AssignExpr) node;
            
            addStringToBuilder(assignExpr.getOperator());
        }else if (isContain(nodeClassPackage,"FieldAccessExpr")) {
            FieldAccessExpr fieldAccessExpr = (FieldAccessExpr) node;
            
        }else if (isContain(nodeClassPackage, "ArrayAccessExpr")) {
            ArrayAccessExpr arrayAccessExpr = (ArrayAccessExpr) node;
            
            addStringToBuilder(arrayAccessExpr.getIndex());
        }else if (isContain(nodeClassPackage, "ArrayCreationExpr")) {
            ArrayCreationExpr arrayCreationExpr = (ArrayCreationExpr) node;
            
            addStringToBuilder(arrayCreationExpr.getElementType());
            if (arrayCreationExpr.getLevels().isEmpty()){
                addStringToBuilder(SplitString.splitUntilUpperCase("ArrayCreationLevel Empty"));
            } else {
                addStringToBuilder(SplitString.splitUntilUpperCase("ArrayCreationLevel NotEmpty"));
            }
            if (arrayCreationExpr.getInitializer().isPresent()) {
                addStringToBuilder(SplitString.splitUntilUpperCase("ArrayInitializerExpr"));
            }
        }else if (isContain(nodeClassPackage, "ArrayInitializerExpr")) {
            ArrayInitializerExpr arrayInitializerExpr = (ArrayInitializerExpr) node;
            
        }else if (isContain(nodeClassPackage, "ArrayCreationLevel")) {
            ArrayCreationLevel arrayCreationLevel = (ArrayCreationLevel) node;
            if (arrayCreationLevel.getDimension().isPresent()){
                addStringToBuilder("Dimension NotEmpty");
            } else {
                addStringToBuilder("Dimension Empty");
            }
        }else if (isContain(nodeClassPackage, "ObjectCreationExpr")) {
            ObjectCreationExpr objectCreationExpr = (ObjectCreationExpr) node;
            
            addStringToBuilder(objectCreationExpr.getType());
            addStringToBuilder(objectCreationExpr.getScope().toString());
            addStringToBuilder(objectCreationExpr.getTypeArguments().toString());
        }else if (isContain(nodeClassPackage, "LambdaExpr")) {
            LambdaExpr lambdaExpr = (LambdaExpr) node;
            
        }else if (isContain(nodeClassPackage, "EnclosedExpr")) {
            EnclosedExpr enclosedExpr = (EnclosedExpr) node;
            
        }else if (isContain(nodeClassPackage, "InstanceOfExpr")) {
            InstanceOfExpr instanceOfExpr = (InstanceOfExpr) node;
            
        }else if (isContain(nodeClassPackage, "MemberValuePair")) {
            MemberValuePair memberValuePair = (MemberValuePair) node;
            
        }else if (isContain(nodeClassPackage, new String[]{"ThisExpr", "SuperExpr"})) {
            
        }else {
            // TODO: If need normalization
            addStringToBuilder(node);
        }
        return mStringBuilder.toString();
    }

    private void addStringToBuilder(Object object) {
        mStringBuilder.append(object.toString());
        mStringBuilder.append(" ");
    }

    private void processThrows(NodeList<ReferenceType> thrownExceptions) {
        if (!thrownExceptions.isEmpty()) {
            addStringToBuilder("Throws");
        }
    }

    public void processModifiers(EnumSet<Modifier> modifierEnumSet) {
        for (Modifier m : modifierEnumSet){
            addStringToBuilder(m.toString());
        }
    }

    private void processLiteralExpr(Node node) {
        String nodeClassPackage = node.getClass().toString();
        String[] nodeClassPackageSplit = node.getClass().toString().split("\\.");
        String nodeClass = nodeClassPackageSplit[nodeClassPackageSplit.length - 1];
        if (isContain(nodeClassPackage, new String[]{"BooleanLiteralExpr","CharLiteralExpr"})) {
            addStringToBuilder(node);
        }else if (isContain(nodeClassPackage, "StringLiteralExpr")) {
            StringLiteralExpr stringLiteralExpr = (StringLiteralExpr) node;
            if (stringLiteralExpr.asString().isEmpty()){
                addStringToBuilder("Empty ");
            }else {
                addStringToBuilder("Not Empty ");
            }
        }else if (isContain(nodeClassPackage, "DoubleLiteralExpr")) {
            DoubleLiteralExpr doubleLiteralExpr = (DoubleLiteralExpr) node;
            if (doubleLiteralExpr.asDouble() == 0.0) {
                addStringToBuilder("Zero ");
            } else {
                addStringToBuilder("Note Zero ");
            }
        }else if (isContain(nodeClassPackage, "IntegerLiteralExpr")) {
            IntegerLiteralExpr integerLiteralExpr = (IntegerLiteralExpr) node;
            if (integerLiteralExpr.asInt() == 0) {
                addStringToBuilder("Zero ");
            } else {
                addStringToBuilder("Note Zero ");
            }
        }else if (isContain(nodeClassPackage, "LongLiteralExpr")) {
            LongLiteralExpr longLiteralExpr = (LongLiteralExpr) node;
            if (longLiteralExpr.asLong() == 0l) {
                addStringToBuilder("Zero ");
            } else {
                addStringToBuilder("Note Zero ");
            }
        }else if (isContain(nodeClassPackage, "NullLiteralExpr")) {
        }
    }

    private String stringPrint(String string) {
        return string;
    }

    private String nodePrint(Node node) {
        return node.toString();
    }

    public boolean isContain(String master, String sub) {
        return master.contains(sub);
    }

    public boolean isContain(String master, String[] sub) {
        for (String s : sub) {
            if (isContain(master, s)){
                return true;
            }
        }
        return false;
    }
}
