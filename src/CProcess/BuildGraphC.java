package CProcess;

import GraphProcess.Graph;
import com.google.common.graph.MutableNetwork;
import org.eclipse.cdt.core.dom.ast.IASTNode;

import java.util.ArrayList;
import java.util.List;

public class BuildGraphC extends CParseUtil implements Graph {
    public List<IASTNode> mVisitedNodes = new ArrayList<>();
    public int mEdgeNumber;
    public MutableNetwork<Object, String> mNetwork;
    public boolean mAll = false;

    private BuildGraphC(String srcFilePath) {
        super(srcFilePath);
    }

    public static BuildGraphC newFromFile(String srcFilePath) {
        BuildGraphC bc = new BuildGraphC(srcFilePath);
        if (bc.mTranslationUnit == null) {
            return null;
        } else {
            return bc;
        }
    }

    public <T extends IASTNode> boolean visitNode(T node) {


        return mAll;
    }



}