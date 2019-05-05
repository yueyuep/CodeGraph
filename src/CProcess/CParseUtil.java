package CProcess;

import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.ast.gnu.cpp.GPPLanguage;
import org.eclipse.cdt.core.index.IIndex;
import org.eclipse.cdt.core.parser.*;
import org.eclipse.cdt.internal.core.index.EmptyCIndex;
import org.eclipse.core.runtime.CoreException;

import java.io.File;
import java.util.HashMap;
import java.util.Map;


public class CParseUtil {
    public String mSrcFilePath;
    public IASTTranslationUnit mTranslationUnit;
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

    public void enableOption(int option){
        options |= option;
    }

    public String getSrcFilePath() {
        return mSrcFilePath;
    }

    public IASTTranslationUnit getTranslationUnit() {
        return mTranslationUnit;
    }

    public int getOptions() {
        return options;
    }
}
