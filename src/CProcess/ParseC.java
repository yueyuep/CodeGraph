package CProcess;

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
            System.out.println(bc.getTranslationUnit());
        }

    }
}
