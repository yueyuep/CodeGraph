package CProcess;

import GraphProcess.Util;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTFunctionDefinition;

import java.io.IOException;
import java.util.ArrayList;

public class CNVD {
    public static void main(String[] args) throws IOException {
        getNVDGraphs();
    }

    public static void getNVDGraphs() throws IOException {
        String dirPath = "../VulDeePecker/CWE-119/source_files/";
        String cgdFile = "cgd/VulDeeNVD399.txt";
        Gson gson = new Gson();
        String json = Util.readFileToString(cgdFile);
        ArrayList<CGD> cgds = gson.fromJson(json, new TypeToken<ArrayList<CGD>>(){}.getType());
        for (CGD cgd : cgds) {
            String filePath = dirPath + cgd.getFileName();
            String id = cgd.getId();
            int line = Integer.parseInt(cgd.getLineNumber());
            boolean label = false;
            if (cgd.getLabel().equals("1")) {
                label = true;
            }
            genSaveNVDGraph(filePath, line, label, id);
        }
    }

    private static void genSaveNVDGraph(String filePath, int line, boolean label, String id) {
        BuildGraphC bc = BuildGraphC.newFromFile(filePath);
        if (bc == null) {
            System.out.println("Null: " + filePath);
            return;
        }
//        CPPASTFunctionDefinition funcDefinition = bc.findFuncDefinition(line);

    }
}
