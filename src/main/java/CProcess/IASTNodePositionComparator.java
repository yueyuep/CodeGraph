package CProcess;

import com.github.javaparser.Position;
import com.github.javaparser.ast.Node;
import org.eclipse.cdt.core.dom.ast.IASTNode;

/**
 * @ Author     ：wxkong
 */
public class IASTNodePositionComparator {

    public static boolean isBeforePosition(IASTNode a, IASTNode b) {
        return compare(a, b) > 0;
    }

    public static boolean isAfterPosition(IASTNode a, IASTNode b) {
        return compare(a, b) < 0;
    }

    public static int compare(IASTNode a, IASTNode b) {
        return new IASTNodeComparator().compare(a, b);
    }

    public static boolean ifSmaller(IASTNode positionSmall, IASTNode positionBig) {
        return compare(positionSmall, positionBig) >= 0;
    }
}
