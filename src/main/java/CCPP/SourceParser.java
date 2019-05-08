/*******************************************************************************
 * Copyright (c) 2014, CriativaSoft, All rights reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3.0 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library.
 *******************************************************************************/
package CCPP;

import CCPP.metadata.*;
import CCPP.metadata.TClass.TClassType;
import org.eclipse.cdt.core.dom.ast.*;
import org.eclipse.cdt.core.dom.ast.IASTEnumerationSpecifier.IASTEnumerator;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTCompositeTypeSpecifier.ICPPASTBaseSpecifier;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTVisibilityLabel;
import org.eclipse.cdt.core.dom.ast.gnu.cpp.GPPLanguage;
import org.eclipse.cdt.core.index.IIndex;
import org.eclipse.cdt.core.model.ITranslationUnit;
import org.eclipse.cdt.core.parser.*;
import org.eclipse.cdt.internal.core.dom.parser.ASTProblem;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTFunctionDefinition;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTTranslationUnit;
import org.eclipse.cdt.internal.core.index.EmptyCIndex;
import org.eclipse.cdt.internal.core.parser.scanner.CharArray;
import org.eclipse.cdt.internal.core.parser.scanner.InternalFileContent;
import org.eclipse.core.runtime.CoreException;

import java.io.File;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;


public class SourceParser {
    
    private final static Logger LOG = Logger.getLogger(LibraryIndex.class.getName()); 
    
    private Set<TClass> classes = new HashSet<TClass>();
    
    private Set<TFunction> globalFunctions = new HashSet<TFunction>();
    
    private Set<TAttribute> globalVariables = new HashSet<TAttribute>();
    
    private Set<TError> errors = new HashSet<TError>();
    
    Map<String, String> definedMacros = new HashMap<String, String>();
    
    private TClass currentClass;
    
    boolean hasFunctionsOverload = false; // 2 functions with same name.
    
    private boolean parseInternalAttrs = false;
    
    private boolean parseComments = true;
    
    private static final String STR_VOID = "void";
    private static final String STR_STATIC = "static";
    private static final String STR_EXTERN = "extern";
    
    private String defaultFileName = "SourceCode.c";
    
    private int options = 0;
    
    // AST variables...
    private IASTCompositeTypeSpecifier curreType;
    private IASTNode curretNode;
    private ICPPASTVisibilityLabel publicScopeLabel = null;
    
    public void parse(String sourceCode){
    	FileContent fileContent = new InternalFileContent(defaultFileName, new CharArray(sourceCode));
    	enableOption(GPPLanguage.OPTION_IS_SOURCE_UNIT);
    	enableOption(ITranslationUnit.AST_SKIP_ALL_HEADERS);
    	parse(fileContent);
    }
    
    public void parse(File file ){
    	FileContent fileContent = FileContent.createForExternalFileLocation(file.getPath());
    	parse(fileContent);
    }
    
    public void parse(FileContent fileContent ){
        
    	long time = System.currentTimeMillis();

    	enableOption(GPPLanguage.OPTION_NO_IMAGE_LOCATIONS);
    	// enableOption(GPPLanguage.OPTION_SKIP_TRIVIAL_EXPRESSIONS_IN_AGGREGATE_INITIALIZERS);
    	
        String[] includePaths = new String[0];
        IScannerInfo info = new ScannerInfo(definedMacros,includePaths);
        IParserLogService log = new DefaultLogService();
        IIndex index = EmptyCIndex.INSTANCE; // or can be null
        //final IIndexManager indexManager= CCorePlugin.getIndexManager();
        //IIndex index = indexManager.getIndex(fTranslationUnit.getCProject(),IIndexManager.ADD_EXTENSION_FRAGMENTS_EDITOR);
        
        IncludeFileContentProvider emptyIncludes = IncludeFileContentProvider.getEmptyFilesProvider();
       
        try {
            // Using: org.eclipse.cdt.internal.core.dom.parser.cpp.GNUCPPSourceParser
        	IASTTranslationUnit translationUnit = GPPLanguage.getDefault().getASTTranslationUnit(fileContent, info, emptyIncludes, index, options , log);
        	
        	navigateTree(translationUnit, 1);
        	
        	collectMacrosAndComments(translationUnit);
        	
        } catch (CoreException e) {
            e.printStackTrace();
        }
        
        
        logFiner("Parser Time: [" + (System.currentTimeMillis() - time) + "]");

        if(LOG.isLoggable(Level.FINER)){

            logFiner("=========== Global =============");

            Set<TAttribute> attributes = globalVariables;
            for (TAttribute node : attributes) {
                logFiner("#" + node);
            }
            
            Set<TFunction> functions = globalFunctions;
            for (TFunction tFunction : functions) {
                logFiner("#"+tFunction);
            }

            logFiner("=========== Errors =============");

            for (TError node : errors) {
                logFiner("#"+node);
            }


            logFiner("=========== Classes =============");

            for (TClass klass : classes) {
                logFiner(klass.toString());
                logFiner("-----------------------");

                attributes = klass.getAttributes();
                for (TAttribute node : attributes) {
                    logFiner(node.toString());
                }
                
                functions = klass.getFunctions();
                for (TFunction tFunction : functions) {
                    logFiner(tFunction.toString());
                }

                logFiner("");
            }
        }
        
    }

    public void logFiner(String s) {
//        LOG.finer(s);
        System.out.println(s);
    }

    protected void collectMacrosAndComments(IASTTranslationUnit translationUnit){

        logFiner("Macro Definitions");
        logFiner("=====================================");

        IASTPreprocessorMacroDefinition[] macroDefinitions = translationUnit.getMacroDefinitions();
    	for (IASTPreprocessorMacroDefinition macro : macroDefinitions) {

    	    logFiner(macro.getClass().getSimpleName() + " -> " + macro);
    		
			if(macro instanceof IASTPreprocessorFunctionStyleMacroDefinition){
				IASTPreprocessorFunctionStyleMacroDefinition fmacro = (IASTPreprocessorFunctionStyleMacroDefinition) macro;
				TFunction function = new TFunction(fmacro.getName().getRawSignature());
				function.setStatic(true);
				function.setReturnType("undefined");
				setSourceLocation(function, macro);
				globalFunctions.add(function);
			}else  if(macro instanceof IASTPreprocessorObjectStyleMacroDefinition){
    			IASTPreprocessorObjectStyleMacroDefinition amacro = (IASTPreprocessorObjectStyleMacroDefinition) macro;
    			TAttribute attribute = new TAttribute(amacro.getName().getRawSignature());
    			attribute.setStatic(true);
    			attribute.setType("undefined");
    			setSourceLocation(attribute, macro);
    			// globalVariables.add(attribute);
    		}
			

		}
    	
    	if(parseComments){
    		
	    	IASTComment[] comments = translationUnit.getComments();
	    	
	    	List<TElement> elements = new LinkedList<TElement>();
	    	elements.addAll(classes);
	    	elements.addAll(globalFunctions);
	
			for (IASTComment iastComment : comments) {
				
				if(!iastComment.isBlockComment()) continue;

				String comment = new String(iastComment.getComment());
				
				// Ignore header...
				if(comment.contains("Copyright") || comment.contains("This library is free software")) continue;
				
		    	for (TElement element : elements) {
		    		
		    		if(element.getLocation() == null || element.getLocation().getStartOffset() == 0) continue;
		    			
		    		int offset = element.getLocation().getStartOffset();
		    		
		    		IASTNodeLocation location = iastComment.getNodeLocations()[0];
		    		
		    		int diff = offset - (location.getNodeOffset() + location.getNodeLength());
		    		
		    		if(diff > 0 && diff < 5){
		    			
		    		    LOG.finest("Doc of: " + element.name());
		    		    LOG.finest(comment);

					}  			
		    		

				}

			}

		}

    }
    
    protected void navigateTree(IASTNode node, int index) {
        
        IASTNode[] children = node.getChildren();
        
        this.curretNode = node;
        
        boolean printContents = true;
        
        if(node instanceof CPPASTTranslationUnit){
            printContents = false;
        }
        
        // Class type declaration... 
        if(node instanceof IASTCompositeTypeSpecifier){
            
            // Its a typedef inside class
            if(curreType != null && curreType != node && curreType.contains(node)){
                if(isPublic(node)){
                    _debug("> typedef inside class: " + curreType.getName().getRawSignature(), index);
                
                    // process typedef
                    IASTCompositeTypeSpecifier lastType = curreType;
                    TClass lastClass = currentClass;
                    curreType = (IASTCompositeTypeSpecifier) node;
                    navigateTree(node, index);
                    
                    // restore current
                    curreType = lastType;
                    currentClass = lastClass;
                return;                  
                }else{ // ignore private typedef
                    return;
                }
            }
            
            curreType = (IASTCompositeTypeSpecifier) node;
            
            publicScopeLabel = null;
            findPublicScopeLabel(curreType);
            
            String name = curreType.getName().getRawSignature();
            
            if(name == null || name.trim().length() == 0){
            	
            	 // In struct the name has in another structure (is a sibling node IASTSimpleDeclaration).
            	 if(curreType.getKey() == IASTCompositeTypeSpecifier.k_struct){
                 	if(curreType.getParent() instanceof IASTSimpleDeclaration){
                 		IASTSimpleDeclaration parentDec = (IASTSimpleDeclaration) curreType.getParent();
                 		if(parentDec.getDeclarators().length > 0){
                 			IASTDeclarator parentNameDec = parentDec.getDeclarators()[0];
                 			name = parentNameDec.getName().toString();
                 		}
                 	}
                 }
            	
            	if(name == null || name.trim().length() == 0){
            		 logFiner("WARN: ClassName Empty ! CurrentLevel ("+index+") File:"+node.getContainingFilename());
            		 return;
            	}
               
            }
            
            currentClass = new TClass(name);
            setSourceLocation(currentClass, node);
            
            if(curreType.getKey() == IASTCompositeTypeSpecifier.k_struct){
            	currentClass.setType(TClassType.STRUCT);
            }
            
            classes.add(currentClass);
        }
        
        // Parent class.
        if(node instanceof ICPPASTBaseSpecifier){
            IASTName name = ((ICPPASTBaseSpecifier)node).getName();
            TClass parent = new TClass(name.getRawSignature());
            currentClass.setParent(parent);
        }
        
        String offset = "";
        try {
            offset = (node.getSyntax() != null ? " (offset: "+node.getFileLocation().getNodeOffset()+","+ node.getFileLocation().getNodeLength()+")" : "");
            printContents = node.getFileLocation().getNodeLength() < 30;
        } catch (ExpansionOverlapsBoundaryException e) {
            e.printStackTrace();
        } catch (UnsupportedOperationException e) {
            offset = "UnsupportedOperationException";
        }
        
        if(LOG.isLoggable(Level.FINEST)){
            String content = (printContents ? node.getRawSignature() : node.getRawSignature().subSequence(0, 5)).toString();
            if(content.length() > 0) content = content.replaceAll("\n", " \\ ");
            logFiner(String.format("%1$"+(index * 2)+ "s", "-") + node.getClass().getSimpleName() +  offset + " -> [[ " +  content + " ]]");
        }
        
        
        // Enum
        if(node instanceof IASTEnumerationSpecifier){
            
            IASTEnumerationSpecifier enumspec = (IASTEnumerationSpecifier) node;
            
            String enumname = enumspec.getName().toString();
            
            if(currentClass == null){
                currentClass = new TClass(enumname);
                setSourceLocation(currentClass, node);
            }
            
            IASTEnumerator[] enumerators = enumspec.getEnumerators();
            
            for (IASTEnumerator iastEnumerator : enumerators) {
                String item = iastEnumerator.getName().toString();
                TAttribute attribute = new TAttribute(enumname, item);
                attribute.setPublic(true);
                attribute.setEnum(true);
                currentClass.add(attribute);
            }
            
            return; // ignore navigate in childs childs..
        }
        
        
        if(isFunction(node)){
            
            TFunction function = convertToFunction(node);
            
            if(function == null) return;
            
            // if not global !
            if(currentClass != null){
                currentClass.add(function);     
            }else{
                globalFunctions.add(function);
                updateCheckOverloadedFunctions();
            }
        }
        
        // Detect Attributes, Variables.
        if(node instanceof IASTDeclarator && ! isFunction(node)){
            
            IASTNode parent = node.getParent();
            
            if(parent instanceof IASTSimpleDeclaration){
            	
            	IASTSimpleDeclaration parentDeclaration = (IASTSimpleDeclaration) parent;
                
                // is a class attribute.
                if(parent.getParent() instanceof IASTCompositeTypeSpecifier){
                    
                    TAttribute attribute = convertToAttr(node);
                    if(attribute == null) return;
                        
                    attribute.setPublic(isPublic(node));
                    
                    _debug("> Class-Variable:" + attribute, index);
                    
                    if(attribute.isPublic() || attribute.isStatic() || currentClass.getType() == TClassType.STRUCT  ||  currentClass.getType() == TClassType.ENUM){
                        currentClass.add(attribute);
                    }
                
                // Body variable (only for .cpp files.)   
                // or: Extern, Struct
                }else {
                	
                	// this can be a Struct(name declaration), ignore... this is handled in 'if'(Class type declaration... )
                	if(parentDeclaration.getDeclSpecifier() instanceof IASTCompositeTypeSpecifier){
                		IASTCompositeTypeSpecifier specifier = (IASTCompositeTypeSpecifier) parentDeclaration.getDeclSpecifier();
                		if(specifier.getKey() == IASTCompositeTypeSpecifier.k_struct){
                			return;
                		}
                		
                	}
                	
                    TAttribute attribute = convertToAttr(node);
                    if(attribute == null) return;
                    
                    // Extern: Relace ClassName
                    if(attribute.isExtern()){
                    	replaceClassName(attribute.getType(), attribute.getName());
                    	return;
                    }
                    
                    if(attribute.name().length() > 2){ // ignore small variables. ex:. for(int i=0)
                    	setSourceLocation(attribute, node);
                    	CPPASTFunctionDefinition findParentDef = findParent(node, CPPASTFunctionDefinition.class);
                    	
                    	// check if inside function def
                    	if(findParentDef != null){
                        	TFunction findParent = findParentTFunction(node);
                        	if(findParent != null){
                        		attribute.setLocal(true);
                        		attribute.setPublic(false);
                        		attribute.setParent(findParent);
                        		findParent.addLocalVariable(attribute);
                        		_debug("> Body-Variable:" + attribute, index);
                        	}
                       }else{
                    		_debug("> Global-Variable:" + attribute, index);
                    		attribute.setStatic(true);
                    		attribute.setPublic(true);
                    		globalVariables.add(attribute);
                    	}

                    }
                    
                }

            }
            
        }
        
        if(node instanceof IASTProblem){
            
            IASTProblem problem = (IASTProblem) node;
            
            int pid = problem.getID();
            
            String message = ASTProblem.getMessage(pid, null);
            
            TError error = new TError(message, pid);
            setSourceLocation(error, node);
            errors.add(error);
            
            _debug("ERROR: " + message, index);
            
        }
        
        // recursive interaction....
        for (IASTNode iastNode : children) {
            navigateTree(iastNode, index + 1);
        }

        // termination of current class
        if(node instanceof IASTCompositeTypeSpecifier || curreType == node){
            curreType = null;
            publicScopeLabel = null;
        }
    }
    
    
    private void replaceClassName(String currentName, String newName){
    	for (TClass tClass : classes) {
			if(tClass.name().equals(currentName)){
				tClass.setName(newName);
				tClass.setType(TClassType.EXTERN);
			}
		}
    }
    
    private void updateCheckOverloadedFunctions(){
    	
    	if(globalFunctions.size() <= 1) return;
    	
    	List<String> names = new ArrayList<String>();
    	Set<String> setnames = new HashSet<String>();
    	
    	for (TFunction tFunction : globalFunctions) {
    		names.add(tFunction.name());
		}
    	
    	setnames.addAll(names);
    	
    	hasFunctionsOverload = setnames.size() != names.size();
    }
   
    public boolean isFunction(IASTNode node){
        return (node instanceof IASTFunctionDeclarator);
    }
    
    private TAttribute convertToAttr( IASTNode node ) {
        
        IASTName name = ((IASTDeclarator) node).getName();
        IASTSimpleDeclaration parentDec  = (IASTSimpleDeclaration) node.getParent(); 
        String signature = parentDec.getDeclSpecifier().getRawSignature();
        String freturn = null;
        
        if(name.toString().length() == 0) return null;
        
        if(signature.length() > 0){
        	
            if(signature.contains(" ")){ // split attr definition (ex: static int attrName)
                String[] split = signature.split(" ");
                freturn = split[split.length - 1];
            }else{
                freturn =  signature;
            }
        }
        
        TAttribute attribute = new TAttribute(freturn, name.toString());
        attribute.setStatic(signature.contains(STR_STATIC));
        attribute.setExtern(signature.contains(STR_EXTERN));
        
        if(node instanceof IASTArrayDeclarator){
            attribute.setArray(true);
        }
        
        return attribute;
    }

    
    public TFunction convertToFunction(IASTNode node){
        
        IASTFunctionDeclarator functionNode = ((IASTFunctionDeclarator) node);
        
        String name = functionNode.getName().getRawSignature();
        
        if(name != null && name.trim().length() != 0){
            
            // Ignore...
            if(name.startsWith("~") || name.startsWith("operator")) return null;
            
            String signatureDec = "";
            String freturn = null;
            
            if(functionNode.getParent() instanceof IASTSimpleDeclaration){ //  parent can be SimpleDeclaration, FunctionDefinition
                IASTSimpleDeclaration parent = (IASTSimpleDeclaration) functionNode.getParent(); 
                signatureDec = parent.getDeclSpecifier().getRawSignature();
            }
            if(functionNode.getParent() instanceof IASTFunctionDefinition){ //  parent can be SimpleDeclaration, FunctionDefinition
                IASTFunctionDefinition parent = (IASTFunctionDefinition) functionNode.getParent(); //  parent can be anoter instance
                signatureDec = parent.getDeclSpecifier().getRawSignature();
            }
            
            if(signatureDec.length() > 0){
                if(signatureDec.contains(" ")){
                    String[] split = signatureDec.split(" ");
                    freturn = split[split.length - 1];
                }else{
                    freturn =  signatureDec;
                }
            }
            
            TFunction function = new TFunction(name, currentClass);
            function.setPublic(isPublic(node));
            
            if(currentClass != null && currentClass.getType() == TClassType.STRUCT) function.setPublic(true);
            
            function.setStatic(signatureDec.contains(STR_STATIC));
            if(freturn != null && !STR_VOID.equals(freturn)){
                function.setReturnType(freturn);
            }
            //System.out.println("SGN ->>> " + signatureDec + ", return: " + freturn);
            setSourceLocation(function, functionNode.getParent());
            
            IASTNode[] fchilds = functionNode.getChildren();
            
            for (IASTNode fchildNode : fchilds) {
                if( fchildNode instanceof IASTParameterDeclaration){
                    IASTParameterDeclaration param = (IASTParameterDeclaration) fchildNode;
                    IASTDeclSpecifier declSpecifier = param.getDeclSpecifier();
                    String type = declSpecifier.getRawSignature();
                    if(!STR_VOID.equals(type)){
                        function.addParam(type, param.getDeclarator().getRawSignature());
                    }
                }
            }
            
            return function;
  
        }
        
        return null;
    }
    
    private void setSourceLocation(TElement element, IASTNode node){
        TElementLocation location = new TElementLocation();
        location.setPath(node.getContainingFilename());
        IASTNodeLocation[] nodeLocations = node.getNodeLocations();
        if(nodeLocations != null && nodeLocations.length > 0){
            IASTNodeLocation nodeLocation = node.getNodeLocations()[0];
            location.setOffset(nodeLocation.getNodeOffset());
            location.setLength(nodeLocation.getNodeLength());
        }
        element.setLocation(location);
        
    }
    
    private <T> T findParent( IASTNode current, Class<T> parentClass) {
    	
    	IASTNode parent = current.getParent();
    	
    	if(parent == null || IASTTranslationUnit.class.isAssignableFrom(parent.getClass())) return null; // finish.
    	
    	if(parentClass.isAssignableFrom(parent.getClass())){
    		return (T) parent;
    	}
    	
    	return findParent(parent, parentClass);
    	
    }
    
    private TFunction findParentTFunction(IASTNode node) {
    	
    	CPPASTFunctionDefinition findParent = findParent(node, CPPASTFunctionDefinition.class);
    	
    	if(findParent != null){
    		String function = findParent.getDeclarator().getName().toString();
    		
    		// TODO: need check currentClass ? (NOTE: currentClass is not cleared !)
    		for (TFunction tFunction : globalFunctions) {
    			
    			if(tFunction.name().equals(function)){ 
    				
    				// NOTE: can be many with name name !!! 
    				if(hasFunctionsOverload){ // is only for performance
    					
    					TFunction converted = convertToFunction(findParent.getDeclarator());
    					
    					if(tFunction.equals(converted)){ // compare name and params types.
    						return tFunction;
    					}
    					
    				}else{
    					return tFunction;
    				}
    				
    			}
			}
    	}
    	
    	return null;
    	
    }
    
    /**
     * Finds and saves the position / in which demarcates the public variables
     */
    private void findPublicScopeLabel( IASTCompositeTypeSpecifier type ) {
        IASTNode[] children = type.getChildren();
        
        for (IASTNode iastNode : children) {
            if(iastNode instanceof ICPPASTVisibilityLabel){
                if(((ICPPASTVisibilityLabel) iastNode).getVisibility() == ICPPASTVisibilityLabel.v_public){
                    publicScopeLabel = (ICPPASTVisibilityLabel) iastNode;
                }
            }
        }
    }

    /**
     * Check if is public based in the location of 'Node Offset' reative to public label.
     * @param current
     * @return
     */
    private boolean isPublic(IASTNode current){
        
        if(curreType == null){
            _debug("is static! no curreType", 1);
            return true; // static !
        }
        
        if(publicScopeLabel == null){
            return false;
        }
        
        if(current.getNodeLocations()[0].getNodeOffset() > publicScopeLabel.getNodeLocations()[0].getNodeOffset()){
            return true;
        }
        
        return false;
    }
    
    private static void _debug(String text, int index){
        if(LOG.isLoggable(Level.FINEST)){
            System.out.println(str_repeat("-", (index * 2)) + " " + text);
        }
    }
    
    		
    private static String str_repeat(String schar, int length){
    	return new String(new char[length]).replace("\0", schar);
    }
    
    public Set<TClass> getClasses() {
        return classes;
    }
    
    public Set<TFunction> getGlobalFunctions() {
        return globalFunctions;
    }
    
    public Set<TAttribute> getGlobalVariables() {
        return globalVariables;
    }
    
    public Set<TError> getErrors() {
        return errors;
    }
    
    /**
     * Must parse internal attributes in body of functions.
     * @param parseInternalAttrs
     */
    public void setParseInternalAttrs( boolean parseInternalAttrs ) {
        this.parseInternalAttrs = parseInternalAttrs;
    }

    public boolean isParseInternalAttrs() {
        return parseInternalAttrs;
    }
    
    public void setDefaultFileName( String defaultFileName ) {
        this.defaultFileName = defaultFileName;
    }
    
    public String addMacro( String key , String value ) {
        return definedMacros.put(key, value);
    }
    
    public void enableOption(int option){
        options |= option;
    }
    

}
