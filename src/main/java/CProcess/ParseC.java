package CProcess;

import org.eclipse.cdt.core.dom.ast.IASTCompoundStatement;
import org.eclipse.cdt.core.dom.ast.IASTDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;

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
