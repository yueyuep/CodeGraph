package CProcess;

import org.eclipse.cdt.core.dom.ast.IASTNode;

public class IASTNodeComparator implements java.util.Comparator<IASTNode> {
    @Override
    public int compare(IASTNode node1, IASTNode node2) {
        int cr = 0;
        int a = node2.getFileLocation().getNodeOffset() - node1.getFileLocation().getNodeOffset();
        if (a != 0) {
            cr = (a < 0) ? 3 : -1;
        } else {
            a = node2.getFileLocation().getNodeLength() - node1.getFileLocation().getNodeLength();
            if (a != 0) {
                cr = (a < 0) ? 2 : -2;
            }
        }
        return cr;
    }
}
