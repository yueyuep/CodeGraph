package CProcess;

import com.github.javaparser.ast.expr.NameExpr;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.ast.gnu.cpp.GPPLanguage;
import org.eclipse.cdt.core.index.IIndex;
import org.eclipse.cdt.core.parser.*;
import org.eclipse.cdt.internal.core.dom.parser.cpp.*;
import org.eclipse.cdt.internal.core.index.EmptyCIndex;
import org.eclipse.core.runtime.CoreException;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;


public class CParseUtil {
    public String mSrcFilePath;
    public IASTTranslationUnit mTranslationUnit;
    public Set<IASTFunctionDefinition> mFunctionDefinitions = new HashSet<>();
    public int options = 0;

    CParseUtil(String srcFilePath) {
        mSrcFilePath = srcFilePath;
        FileContent fileContent = FileContent.createForExternalFileLocation(srcFilePath);
        mTranslationUnit = parse(fileContent);
    }

    public void parse(File file ){
    	FileContent fileContent = FileContent.createForExternalFileLocation(file.getPath());
    	parse(fileContent);
    }

    public IASTTranslationUnit parse(FileContent fileContent ){
        enableOption(GPPLanguage.OPTION_NO_IMAGE_LOCATIONS);
        String[] includePaths = new String[0];
        Map<String, String> definedMacros = new HashMap<>();
        IScannerInfo info = new ScannerInfo(definedMacros, includePaths);
        IParserLogService log = new DefaultLogService();
        IIndex index = EmptyCIndex.INSTANCE; // or can be null
        IncludeFileContentProvider emptyIncludes = IncludeFileContentProvider.getEmptyFilesProvider();
        try {
            return GPPLanguage.getDefault().getASTTranslationUnit(fileContent, info, emptyIncludes, index, options, log);
        } catch (Exception e) {
            return null;
        }
    }

    public <T extends IASTNode> void collectFunctionDecls(T node) {
        for (IASTNode child : node.getChildren()) {
            if (child instanceof IASTFunctionDefinition) {
                mFunctionDefinitions.add((IASTFunctionDefinition) child);
            } else {
                collectFunctionDecls(child);
            }
        }
    }


    public void enableOption(int option){
        options |= option;
    }

    public int getOptions() {
        return options;
    }

    public String getSrcFilePath() {
        return mSrcFilePath;
    }

    public IASTTranslationUnit getTranslationUnit() {
        return mTranslationUnit;
    }

    public Set<IASTFunctionDefinition> getFunctionDefinitions() {
        return mFunctionDefinitions;
    }

//    public List<IASTNode> getAssignsOrStmtsContains(IASTNode parent, IASTName variableName) {
//        List<IASTNode> containNodes = new ArrayList<>();
//        List<Class> nodeTypes = Arrays.asList(CPPASTIfStatement.class, CPPASTWhileStatement.class, CPPASTDoStatement.class,
//                CPPASTForStatement.class, CPPASTSwitchStatement.class, CPPASTTryBlockStatement.class, CPPASTLambdaExpression.class);
//        for (Class type : nodeTypes) {
//            containNodes.addAll(getNodesContains(parent, variableName, type));
//        }
//        List<Node> toRemove = new ArrayList<>();
//        for (Node node : containNodes) {
//            for (Node other : containNodes) {
//                if (node.equals(other)) {
//                    continue;
//                }
//                if (isParentContains(node, other, Node.class)) {
//                    toRemove.add(other);
//                } else if (isParentContains(other, node, Node.class)) {
//                    toRemove.add(node);
//                }
//            }
//        }
//        containNodes.removeAll(toRemove);
//
//        containNodes.addAll(getSpecificAssignExpr(parent, variableName).stream()
//                .filter(assignExpr -> !isParentsContains(containNodes, assignExpr, AssignExpr.class))
//                .collect(Collectors.toList()));
//        containNodes.sort(new NodeComparator());
//        return containNodes;
//    }

    public <T extends IASTNode> List<T> getNodesContains(IASTNode parent, IASTName simpleName, Class<T> tClass) {
        return findAll(parent, tClass)
                .stream()
                .filter(node -> findAll(node, IASTName.class).contains(simpleName))
                .collect(Collectors.toList());
    }

    public <T extends IASTNode> List<T> findAll(IASTNode node, Class<T> tClass) {
        List<T> nodes = new ArrayList<>();
        for (IASTNode child : node.getChildren()) {
            if (child.getClass().equals(tClass)) {
                nodes.add((T) child);
            }
            nodes.addAll(findAll(child, tClass));
        }
        return nodes;
    }
}
