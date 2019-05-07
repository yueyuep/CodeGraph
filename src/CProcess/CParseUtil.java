package CProcess;

import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.ast.gnu.cpp.GPPLanguage;
import org.eclipse.cdt.core.index.IIndex;
import org.eclipse.cdt.core.parser.*;
import org.eclipse.cdt.internal.core.index.EmptyCIndex;
import org.eclipse.core.runtime.CoreException;

import java.io.File;
import java.util.*;


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
        } catch (CoreException e) {
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
}
