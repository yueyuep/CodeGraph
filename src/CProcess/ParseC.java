package CProcess;

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
        List<String> files = new ArrayList<>();
        files.add(fcpp);
        files.add(fc);

        for (String file : files) {
            BuildGraphC bc = BuildGraphC.newFromFile(file);
            assert bc != null;
            bc.initNetwork();
            IASTTranslationUnit tu = bc.getTranslationUnit();
            System.out.println(file);
//            System.out.println(tu);
            for (IASTFunctionDefinition dec : bc.getFunctionDefinitions()) {
//                System.out.println(dec);
                if (dec.getDeclarator().getName().toString().equals("goodB2G")) {
                    bc.visitNode(dec);
                }
            }
        }

    }
}
