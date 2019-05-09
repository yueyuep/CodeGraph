package CProcess;

import org.eclipse.cdt.core.dom.ast.*;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTBinaryExpression;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTFunctionDefinition;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTName;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTTranslationUnit;

import java.util.ArrayList;
import java.util.List;

/**
 * @ Author     ：wxkong
 */
public class ParseC {
    public static void main(String[] args) {
        String fcpp = "cf/CWE401.cpp";
        String fc = "cf/CWE761.c";
        String fc1 = "cf/10-476_test.c";
        String fc2 = "cf/63-119-120_test.c";
        String fc3 = "cf/0_test.c";
        List<String> files = new ArrayList<>();
        files.add(fc3);
        files.add(fc);
        files.add(fc2);
        files.add(fc1);
        files.add(fcpp);

        for (String file : files) {
            BuildGraphC bc = BuildGraphC.newFromFile(file);
            assert bc != null;
            bc.initNetwork();
            System.out.println(file);
            IASTTranslationUnit tu = bc.getTranslationUnit();
            int len = tu.getFileLocation().getNodeLength();
            int off = tu.getFileLocation().getNodeOffset();
            int s = tu.getFileLocation().getStartingLineNumber();
            int e = tu.getFileLocation().getEndingLineNumber();
            List<CPPASTName> fds = bc.findAll(tu, CPPASTName.class);
            List<IASTName> names = bc.findAll(tu, IASTName.class);
            for (IASTName name : names) {
                List<CPPASTBinaryExpression> assigns = bc.getSpecificAssignExpr(tu, name);
                len = name.getFileLocation().getNodeLength();
                off = name.getFileLocation().getNodeOffset();
                s = name.getFileLocation().getStartingLineNumber();
                e = name.getFileLocation().getEndingLineNumber();
                System.out.println(len);
            }
            for (IASTFunctionDefinition dec : bc.getFunctionDefinitions()) {
                if (dec.getBody() instanceof IASTCompoundStatement) {
                    IASTCompoundStatement body = (IASTCompoundStatement) dec.getBody();
                    if (body.getStatements().length == 0) {
                        System.out.println("O body");
                        continue;
                    }
                }
                bc.visitNode(dec);
                System.out.println("END=============");
            }
        }

    }
}
