package CProcess;

import GraphProcess.Graph;
import GraphProcess.Util;
import com.google.common.graph.MutableNetwork;
import com.google.common.graph.NetworkBuilder;
import org.eclipse.cdt.core.dom.ast.*;
import org.eclipse.cdt.core.dom.ast.cpp.*;
import org.eclipse.cdt.internal.core.dom.parser.cpp.*;

import java.util.ArrayList;
import java.util.List;

import static GraphProcess.Util.isContain;
import static GraphProcess.Util.readFileToArrayList;

public class BuildGraphC extends CParseUtil implements Graph {
    public List<IASTNode> mVisitedNodes = new ArrayList<>();
    public int mEdgeNumber;
    public MutableNetwork<Object, String> mNetwork;
    public boolean mAll = true;
    private IASTFunctionDefinition mFunctionDefinition;

    private BuildGraphC(String srcFilePath) {
        super(srcFilePath);
    }

    public static BuildGraphC newFromFile(String srcFilePath) {
        BuildGraphC bc = new BuildGraphC(srcFilePath);
        if (bc.mTranslationUnit == null) {
            return null;
        } else {
            bc.collectFunctionDecls(bc.mTranslationUnit);
            return bc;
        }
    }

    public void initNetwork() {
        mVisitedNodes.clear();
        mNetwork = NetworkBuilder.directed().allowsParallelEdges(true).allowsSelfLoops(true).build();
        mEdgeNumber = 0;
    }

    public <T extends IASTNode> boolean visitNode(T node) {
        // 先visit，再 || mAll
        if (node == null || node instanceof IASTComment) {
            return false;
        }
//        System.out.println(node.toString());
        boolean add = mAll;
        if (mVisitedNodes.contains(node)) {
            return add;
        }
        mVisitedNodes.add(node);
        String nodeClassPackage = node.getClass().toString();
        String nodeClass = Util.getClassLastName(nodeClassPackage);
        {
            if (node instanceof IASTTranslationUnit) {
                IASTTranslationUnit t = (IASTTranslationUnit) node;
                return isAddChildAndNext(add, t) || mAll;
            }
            if (node instanceof CPPASTFunctionDefinition) {
                CPPASTFunctionDefinition fd = (CPPASTFunctionDefinition) node;
                boolean declarator = visitNode(fd.getDeclarator());
                boolean body = visitNode(fd.getBody());
                addChildNode(fd, fd.getDeclSpecifier());
                addChildNode(fd, fd.getDeclarator());
                addChildNode(fd, fd.getBody());
                addNextNode(fd.getDeclSpecifier(), fd.getDeclarator());
                addNextNode(fd.getDeclarator(), fd.getBody());
                add = declarator || body;
                mFunctionDefinition = fd;
                return add || mAll;
            }
            if (node instanceof CPPASTSimpleDeclSpecifier) {
                CPPASTSimpleDeclSpecifier ds = (CPPASTSimpleDeclSpecifier) node;
                boolean decl = visitNode(ds.getDeclTypeExpression());
                if (decl) {
                    addChildNode(ds, ds.getDeclTypeExpression());
                }
                add = add || mAll;
                if (add) {
                    addChildToken(ds, "Type " + ds.getType());
                }
                return add;
            }
            if (node instanceof CPPASTTypeId) {
                CPPASTTypeId t = (CPPASTTypeId) node;
                boolean s = visitNode(t.getDeclSpecifier());
                boolean d = visitNode(t.getAbstractDeclarator());
                if (s) {
                    addChildNode(t, t.getDeclSpecifier());
                }
                if (d) {
                    addChildNode(t, t.getAbstractDeclarator());
                }
                return s || d || mAll;
            }
            if (node instanceof CPPASTNamedTypeSpecifier) {
                CPPASTNamedTypeSpecifier t = (CPPASTNamedTypeSpecifier) node;
                if (visitNode(t.getName())) {
                    addChildNode(t, t.getName());
                    add = true;
                }
                return add || mAll;
            }
            if (node instanceof CPPASTAliasDeclaration) {
                CPPASTAliasDeclaration a = (CPPASTAliasDeclaration) node;
                if (mAll) {
                    addChildNode(a, a.getMappingTypeId());
                    addChildNode(a, a.getAlias());
                    addNextNode(a.getMappingTypeId(), a.getAlias());
                }
                return mAll;
            }
            if (node instanceof CPPASTFunctionDeclarator) {
                CPPASTFunctionDeclarator fdTor = (CPPASTFunctionDeclarator) node;
                fdTor.getExceptionSpecification();
                boolean name = visitNode(fdTor.getName());
                addChildNode(fdTor, fdTor.getName());
                IASTNode before = fdTor.getName();
                for (IASTNode child : fdTor.getParameters()) {
                    if (visitNode(child)) {
                        addChildNode(fdTor, child);
                        if (before != null) {
                            addNextNode(before, child);
                        }
                        before = child;
                    }
                }
                add = name;
                return add || mAll;
            }
            if (node instanceof CPPASTParameterDeclaration) {
                CPPASTParameterDeclaration pd = (CPPASTParameterDeclaration) node;
                boolean declarator = visitNode(pd.getDeclarator());
                if (declarator) {
                    addChildNode(pd, pd.getDeclarator());
                    addChildNode(pd, pd.getDeclSpecifier());
//                    visitNode(pd.getDeclSpecifier());
                    addNextNode(pd.getDeclSpecifier(), pd.getDeclarator());
                }
                add = declarator;
                return add || mAll;
            }
            if (node instanceof CPPASTCompoundStatement) {
                CPPASTCompoundStatement c = (CPPASTCompoundStatement) node;
                add = mAll;
                for (IASTStatement s : c.getStatements()) {
                    boolean child = visitNode(s);
                    if (child) {
                        addChildNode(c, s);
                        add = true;
                    }
                }
                return add;
            }
            if (node instanceof IASTBreakStatement) {
                return mAll;
            }
            if (node instanceof IASTContinueStatement) {
                return mAll;
            }
            if (node instanceof CPPASTDeclarationStatement) {
                CPPASTDeclarationStatement ds = (CPPASTDeclarationStatement) node;
                boolean dec = visitNode(ds.getDeclaration());
                if (dec) {
                    addChildNode(ds, ds.getDeclaration());
                }
                return dec || mAll;
            }
            if (node instanceof CPPASTSimpleDeclaration) {
                CPPASTSimpleDeclaration dc = (CPPASTSimpleDeclaration) node;
                for (IASTDeclarator dor : dc.getDeclarators()) {
                    boolean should = visitNode(dor);
                    add = add || should;
                    if (should) {
                        addChildNode(dc, dor);
                        addNextNode(dc.getDeclSpecifier(), dor);
                    }
                }
                if (add) {
                    addChildNode(dc, dc.getDeclSpecifier());
                }
                return add || mAll;
            }
            if (node instanceof CPPASTArrayDeclarator) {
                CPPASTArrayDeclarator ar = (CPPASTArrayDeclarator) node;
                ar.getArrayModifiers();
                add = processASTDeclarator(ar, add);
                if (add) {
                    IASTNode before = null;
                    for (IASTArrayModifier m : ar.getArrayModifiers()) {
                        addChildNode(ar, m);
                        if (before != null) {
                            addNextNode(before, m);
                        }
                        before = m;
                    }
                    if (before != null) {
                        addNextNode(before, ar.getName());
                    }
                }
                return add;
            }
            if (node instanceof CPPASTDeclarator) {
                return processASTDeclarator((CPPASTDeclarator) node, add);
            }
            if (node instanceof CPPASTConstructorInitializer) {
                CPPASTConstructorInitializer c = (CPPASTConstructorInitializer) node;
                return isAddChildAndNext(add, c) || mAll;
            }
            if (node instanceof IASTEqualsInitializer) {
                IASTEqualsInitializer e = (IASTEqualsInitializer) node;
                IASTNode before = null;
                add = visitNode(e.getInitializerClause());
                if (add) {
                    addChildNode(e, e.getInitializerClause());
                    before = e.getInitializerClause();
                }
                for (IASTNode child : e.getChildren()) {
                    if (!child.equals(e.getInitializerClause()) && visitNode(child)) {
                        add = true;
                        addChildNode(e, child);
                        if (before != null) {
                            addNextNode(before, child);
                        }
                        before = child;
                    }
                }
                return add || mAll;
            }
            if (node instanceof IASTInitializer) {
                IASTInitializer init = (IASTInitializer) node;
                return isAddChildAndNext(add, init) || mAll;
            }
            if (node instanceof CPPASTExpressionStatement) {
                CPPASTExpressionStatement e = (CPPASTExpressionStatement) node;
                boolean expr = visitNode(e.getExpression());
                if (expr) {
                    addChildNode(e, e.getExpression());
                }
                return expr || mAll;
            }
            if (node instanceof CPPASTBinaryExpression) {
                CPPASTBinaryExpression b = (CPPASTBinaryExpression) node;
                String operator = getOperatorString(b.getOperator());
                if (b.getOperator() == 17) { // Operator 17: =
                    boolean assigned = visitNode(b.getOperand1());
                    if (assigned) {
                        addChildNode(b, b.getOperand1());
                        visitNode(b.getOperand2());
                        addChildNode(b, b.getOperand2());
                        addChildToken(b, operator);
                        addNextNode(b.getOperand1(), operator);
                        addNextNode(operator, b.getOperand2());
                        add = true;
                    } else {
                        boolean equals = visitNode(b.getOperand2());
                        if (equals) {
                            addChildNode(b, b.getOperand2());
                        }
                        add = equals;
                    }
                } else {
                    boolean o1 = visitNode(b.getOperand1());
                    boolean o2 = visitNode(b.getOperand2());
                    if (o1) {
                        addChildNode(b, b.getOperand1());
                    }
                    if (o2) {
                        addChildNode(b, b.getOperand2());
                    }
                    if (o1 && o2) {
                        addChildToken(b, operator);
                        addNextNode(b.getOperand1(), operator);
                        addNextNode(operator, b.getOperand2());
                    }
                    add = o1 || o2;
                }
                return add || mAll;
            }
            if (node instanceof CPPASTUnaryExpression) {
                CPPASTUnaryExpression u = (CPPASTUnaryExpression) node;
                if (visitNode(u.getOperand())) {
                    addChildNode(u, u.getOperand());
                    String operator = getOperatorString(u.getOperator());
                    addChildToken(u, operator);
                    addNextNode(operator, u.getOperand());
                    add = true;
                }
                return add || mAll;
            }
            if (node instanceof CPPASTArraySubscriptExpression) {
                CPPASTArraySubscriptExpression as = (CPPASTArraySubscriptExpression) node;
                boolean arrayExpr = visitNode(as.getArrayExpression());
                boolean tmpAll = mAll;
                if (arrayExpr) {
                    addChildNode(as, as.getArrayExpression());
                    addNextNode(as.getArrayExpression(), as.getArgument());
                    mAll = true;
                }
                boolean argument = visitNode(as.getArgument());
                if (argument) {
                    addChildNode(as, as.getArgument());
                }
                mAll = tmpAll;
                return arrayExpr || argument || mAll;
            }
            if (node instanceof CPPASTIdExpression) {
                CPPASTIdExpression id = (CPPASTIdExpression) node;
                boolean name = visitNode(id.getName());
                if (name) {
                    addChildNode(id, id.getName());
                }
                return name || mAll;
            }
            if (node instanceof CPPASTWhileStatement) {
                CPPASTWhileStatement w = (CPPASTWhileStatement) node;
                boolean body = visitNode(w.getBody());
                if (body) {
                    addChildNode(w, w.getBody());
                    visitNode(w.getCondition());
                    addChildNode(w, w.getCondition());
                    visitNode(w.getConditionDeclaration());
                    addChildNode(w, w.getConditionDeclaration());
                    add = true;
                } else {
                    boolean cond = visitNode(w.getCondition());
                    boolean conD = visitNode(w.getConditionDeclaration());
                    if (cond) {
                        addChildNode(w, w.getConditionDeclaration());
                    }
                    if (conD) {
                        addChildNode(w, w.getConditionDeclaration());
                    }
                    add = cond || conD;
                }
                return add || mAll;
            }
            if (node instanceof CPPASTNewExpression) {
                CPPASTNewExpression n = (CPPASTNewExpression) node;
                if (visitNode(n.getInitializer())) {
                    addChildNode(n, n.getInitializer());
                    addNextNode(n.getTypeId(), n.getInitializer());
                    add = true;
                }
                add = add || mAll;
                if (add) {
                    addChildNode(n, n.getTypeId());
                }
                return add;
            }
            if (node instanceof CPPASTFunctionCallExpression) {
                CPPASTFunctionCallExpression fc = (CPPASTFunctionCallExpression) node;
                boolean name = visitNode(fc.getFunctionNameExpression());
                IASTNode before = null;
                if (name) {
                    addChildNode(fc, fc.getFunctionNameExpression());
                    add = true;
                    before = fc.getFunctionNameExpression();
                }
                boolean tmpAll = mAll;
                if (add) {
                    mAll = true;
                }
                for (IASTInitializerClause init : fc.getArguments()) {
                    if (visitNode(init)) {
                        addChildNode(fc, init);
                        if (before != null) {
                            addNextNode(before, init);
                        }
                        before = init;
                        add = true;
                    }
                }
                mAll = tmpAll;
                return add || mAll;
            }
            if (node instanceof CPPASTLambdaExpression) {
                CPPASTLambdaExpression l = (CPPASTLambdaExpression) node;
                boolean f = visitNode(l.getBody());
                boolean d = visitNode(l.getDeclarator());
                add = f || d || mAll;
                if (add) {
                    addChildNode(l, l.getDeclarator());
                    addChildNode(l, l.getBody());
                    addNextNode(l.getDeclarator(), l.getBody());
                }
                for (IASTNode child : l.getChildren()) {
                    if (!child.equals(l.getDeclarator()) && !child.equals(l.getBody())
                            && visitNode(child)) {
                        add = true;
                        addChildNode(l, child);
                    }
                }
                return add;
            }
            if (node instanceof CPPASTPointerToMember) {
                CPPASTPointerToMember p = (CPPASTPointerToMember) node;
                boolean name = visitNode(p.getName());
                if (name) {
                    addChildNode(p, p.getName());
                    add = true;
                }
                return add || mAll;
            }
            if (node instanceof ICPPASTStaticAssertDeclaration) {
                ICPPASTStaticAssertDeclaration a = (ICPPASTStaticAssertDeclaration) node;
                boolean d = visitNode(a.getCondition());
                if (d) {
                    addChildNode(a, a.getCondition());
                    addChildNode(a, a.getMessage());
                    addNextNode(a.getCondition(), a.getMessage());
                }
                return d || mAll;
            }
            if (node instanceof CPPASTFieldReference) {
                CPPASTFieldReference fr = (CPPASTFieldReference) node;
                boolean owner = visitNode(fr.getFieldOwner());
                boolean name = visitNode(fr.getFieldName());
                add = owner || name || mAll;
                if (add) {
                    addChildNode(fr, fr.getFieldOwner());
                    addChildNode(fr, fr.getFieldName());
                    addNextNode(fr.getFieldOwner(), fr.getFieldName());
                }
                return add;
            }
            if (node instanceof CPPASTIfStatement) {
                CPPASTIfStatement ifs = (CPPASTIfStatement) node;
                boolean c = visitNode(ifs.getConditionExpression());
                if (c) {
                    addChildNode(ifs, ifs.getConditionExpression());
                }
                boolean cd = visitNode(ifs.getConditionDeclaration());
                if (cd) {
                    addChildNode(ifs, ifs.getConditionDeclaration());
                }
                boolean then = visitNode(ifs.getThenClause());
                if (then) {
                    addChildNode(ifs, ifs.getThenClause());
                }
                boolean el = visitNode(ifs.getElseClause());
                if (el) {
                    addChildNode(ifs, ifs.getElseClause());
                }
                boolean init = visitNode(ifs.getInitializerStatement());
                if (init) {
                    addChildNode(ifs, ifs.getInitializerStatement());
                }
                return  c || cd || then || el || init || mAll;
            }
            if (node instanceof CPPASTCastExpression) {
                CPPASTCastExpression ca = (CPPASTCastExpression) node;
                String operator = getOperatorString(ca.getOperator());
                if (visitNode(ca.getOperand())) {
                    addChildNode(ca, ca.getOperand());
                    addChildToken(ca, operator);
                    addChildNode(ca, ca.getTypeId());
                    addNextNode(ca.getTypeId(), operator);
                    addNextNode(operator, ca.getOperand());
                    add = true;
                }
                return add || mAll;
            }
            if (node instanceof IASTConditionalExpression) {
                IASTConditionalExpression c = (IASTConditionalExpression) node;
                boolean logical = visitNode(c.getLogicalConditionExpression());
                boolean positive = visitNode(c.getPositiveResultExpression());
                boolean negative = visitNode(c.getNegativeResultExpression());
                if (logical) {
                    addChildNode(c, c.getLogicalConditionExpression());
                }
                if (positive) {
                    addChildNode(c, c.getPositiveResultExpression());
                }
                if (negative) {
                    addChildNode(c, c.getNegativeResultExpression());
                }
                return logical || positive || negative || mAll;
            }
            if (node instanceof CPPASTCapture) {
                CPPASTCapture c = (CPPASTCapture) node;
                boolean id = visitNode(c.getIdentifier());
                if (id) {
                    addChildNode(c, c.getIdentifier());
                }
                add = id;
                for (IASTNode child : c.getChildren()) {
                    if (!child.equals(c.getIdentifier()) && visitNode(child)) {
                        add = true;
                        addChildNode(c, child);
                    }
                }
                return add || mAll;
            }
            if (node instanceof CPPASTDeleteExpression) {
                CPPASTDeleteExpression d = (CPPASTDeleteExpression) node;
                boolean expr = visitNode(d.getOperand());
                if (expr) {
                    addChildNode(d, d.getOperand());
                }
                return expr || mAll;
            }
            if (node instanceof ICPPASTForStatement) {
                CPPASTForStatement f = (CPPASTForStatement) node;
                boolean init = visitNode(f.getInitializerStatement());
                if (init) {
                    addChildNode(f, f.getInitializerStatement());
                }
                boolean iter = visitNode(f.getIterationExpression());
                if (iter) {
                    addChildNode(f, f.getIterationExpression());
                }
                boolean cond = visitNode(f.getConditionExpression());
                if (cond) {
                    addChildNode(f, f.getConditionExpression());
                }
                boolean conD = visitNode(f.getConditionDeclaration());
                if (conD) {
                    addChildNode(f, f.getConditionDeclaration());
                }
                boolean body = visitNode(f.getBody());
                if (body) {
                    addChildNode(f, f.getBody());
                }
                return init || iter || cond || conD || body || mAll;
            }
            if (node instanceof CPPASTTryBlockStatement) {
                CPPASTTryBlockStatement t = (CPPASTTryBlockStatement) node;
                boolean tb = visitNode(t.getTryBody());
                if (tb) {
                    addChildNode(t, t.getTryBody());
                    add = true;
                }
                for (ICPPASTCatchHandler ch : t.getCatchHandlers()) {
                    if (visitNode(ch)) {
                        addChildNode(t, ch);
                        add = true;
                    }
                }
                return add || mAll;
            }
            if (node instanceof CPPASTSwitchStatement) {
                CPPASTSwitchStatement s = (CPPASTSwitchStatement) node;
                boolean cond = visitNode(s.getControllerDeclaration());
                boolean cone = visitNode(s.getControllerExpression());
                boolean init = visitNode(s.getInitializerStatement());
                boolean body = visitNode(s.getBody());
                if (cond) {
                    addChildNode(s, s.getControllerDeclaration());
                }
                if (cone) {
                    addChildNode(s, s.getControllerExpression());
                }
                if (init) {
                    addChildNode(s, s.getInitializerStatement());
                }
                if (body) {
                    addChildNode(s, s.getBody());
                }
                return cond || cone || init || body;
            }
            if (node instanceof IASTCaseStatement) {
                IASTCaseStatement c = (IASTCaseStatement) node;
                boolean expr = visitNode(c.getExpression());
                if (expr) {
                    addChildNode(c, c.getExpression());
                    add = true;
                }
                for (IASTNode child : c.getChildren()) {
                    if (!child.equals(c.getExpression()) && visitNode(child)) {
                        addChildNode(c, child);
                        add = true;
                    }
                }
                return add || mAll;
            }
            if (node instanceof ICPPASTFunctionWithTryBlock) {
                ICPPASTFunctionWithTryBlock ftb = (ICPPASTFunctionWithTryBlock) node;
                return isAddChild(add, ftb) || mAll;
            }
            if (node instanceof CPPASTCatchHandler) {
                CPPASTCatchHandler c = (CPPASTCatchHandler) node;
                boolean body = visitNode(c.getCatchBody());
                if (body) {
                    add = true;
                    addChildNode(c, c.getCatchBody());
                    visitNode(c.getDeclaration());
                    addChildNode(c, c.getDeclaration());
                    addNextNode(c.getDeclaration(), c.getCatchBody());
                }
                return add || mAll;
            }
            if (node instanceof CPPASTUsingDeclaration) {
                CPPASTUsingDeclaration u = (CPPASTUsingDeclaration) node;
                boolean name = visitNode(u.getName());
                if (name) {
                    addChildNode(u, u.getName());
                }
                return name || mAll;
            }
            if (node instanceof CPPASTDoStatement) {
                CPPASTDoStatement d = (CPPASTDoStatement) node;
                boolean cond = visitNode(d.getCondition());
                boolean body = visitNode(d.getBody());
                if (body) {
                    addChildNode(d, d.getBody());
                    addChildNode(d, d.getCondition());
                    addNextNode(d.getCondition(), d.getBody());
                } else if (cond){
                    addChildNode(d, d.getCondition());
                }
                return body || cond || mAll;
            }
            if (node instanceof CPPASTGotoStatement) {
                CPPASTGotoStatement g = (CPPASTGotoStatement) node;
                boolean name = visitNode(g.getName());
                if (name) {
                    addChildNode(g, g.getName());
                }
                return name || mAll;
            }
            if (node instanceof CPPASTLabelStatement) {
                CPPASTLabelStatement label = (CPPASTLabelStatement) node;
                boolean name = visitNode(label.getName());
                if (name) {
                    addChildNode(label, label.getName());
                }
                return name || mAll;
            }
            if (node instanceof CPPASTNullStatement) {
                CPPASTNullStatement n = (CPPASTNullStatement) node;
                return isAddChild(add, n) || mAll;
            }
            if (node instanceof CPPASTReturnStatement) {
                CPPASTReturnStatement r = (CPPASTReturnStatement) node;
                boolean value = visitNode(r.getReturnValue());
                boolean arg = visitNode(r.getReturnArgument());
                if (value) {
                    addChildNode(r, r.getReturnValue());
                }
                if (arg) {
                    addChildNode(r, r.getReturnArgument());
                }
                addReturnTo(r, mFunctionDefinition);
                return value || arg || mAll;
            }
            if (node instanceof CPPASTLiteralExpression) {
                CPPASTLiteralExpression literal = (CPPASTLiteralExpression) node;
                literal.getKind();
                literal.getOperatorName();
                literal.getValue();
                literal.getValueCategory();
            }
            if (node instanceof CPPASTName) {
                CPPASTName n = (CPPASTName) node;
                return mAll;
            }
            {
                return isAddChildAndNext(add, node) || mAll;
            }
        }
    }

    public void addReturnTo(CPPASTReturnStatement r, IASTFunctionDefinition functionDefinition) {
        putEdge(r, functionDefinition, EDGE_RETURNS_TO);
    }

    public boolean processASTDeclarator(CPPASTDeclarator node, boolean add) {
        boolean name = visitNode(node.getName());
        if (name) {
            addChildNode(node, node.getName());
            IASTNode before = null;
            for (IASTPointerOperator po : node.getPointerOperators()) {
                addChildNode(node, po);
                if (before != null) {
                    addNextNode(before, po);
                }
                before = po;
            }
            if (before != null) {
                addNextNode(before, node.getName());
            }
        }
        add = name || mAll;
        if (node.getInitializer() != null) {
            if (name) {
                boolean tmpAll = mAll;
                mAll = true;
                visitNode(node.getInitializer());
                addChildNode(node, node.getInitializer());
                String initialize = "initialize";
                addChildToken(node, initialize);
                addNextNode(node.getName(), initialize);
                addNextNode(initialize, node.getInitializer());
                mAll = tmpAll;
            } else {
                boolean init = visitNode(node.getInitializer());
                if (init) {
                    addChildNode(node, node.getInitializer());
                    add = true;
                }
            }
        }
        return add;
    }

    public boolean isAddChildAndNext(boolean add, IASTNode node) {
        IASTNode before = null;
        for (IASTNode child : node.getChildren()) {
            if (visitNode(child)) {
                add = true;
                addChildNode(node, child);
                if (before != null) {
                    addNextNode(before, child);
                }
                before = child;
            }
        }
        return add;
    }

    public boolean isAddChild(boolean add, IASTNode node) {
        for (IASTNode child : node.getChildren()) {
            if (visitNode(child)) {
                add = true;
                addChildNode(node, child);
            }
        }
        return add;
    }

    public String getOperatorString(int operator) {
        return "Operator " + operator;
    }

    public void addNextNode(Object before, Object after) {
        putEdge(before, after, EDGE_NEXT_TOKEN);
    }

    public void addChildToken(IASTNode parent, Object child) {
        putEdge(parent, child, EDGE_CHILD_TOKEN);
    }

    public void addChildNode(IASTNode parent, IASTNode child) {
        putEdge(parent, child, EDGE_CHILD_NODE);
    }

    public void putEdge(Object nodeU, Object nodeV, String edgeType) {
        if (nodeU == null || nodeV == null) {
            return;
        }
        mNetwork.addEdge(nodeU, nodeV, edgeType + "_" + mEdgeNumber);
        mEdgeNumber = mEdgeNumber + 1;
    }

    public MutableNetwork<Object, String> getNetwork() {
        return mNetwork;
    }
}