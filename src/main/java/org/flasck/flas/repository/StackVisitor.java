package org.flasck.flas.repository;

import java.util.LinkedList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.ApplyExpr;
import org.flasck.flas.commonBase.ConstPattern;
import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.commonBase.MemberExpr;
import org.flasck.flas.commonBase.NumericLiteral;
import org.flasck.flas.commonBase.Pattern;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.commonBase.names.VarName;
import org.flasck.flas.hsi.HSIVisitor;
import org.flasck.flas.hsi.Slot;
import org.flasck.flas.hsi.TreeOrderVisitor;
import org.flasck.flas.parsedForm.ActionMessage;
import org.flasck.flas.parsedForm.AgentDefinition;
import org.flasck.flas.parsedForm.AnonymousVar;
import org.flasck.flas.parsedForm.AssignMessage;
import org.flasck.flas.parsedForm.ConstructorMatch;
import org.flasck.flas.parsedForm.ContractDecl;
import org.flasck.flas.parsedForm.ContractMethodDecl;
import org.flasck.flas.parsedForm.CurrentContainer;
import org.flasck.flas.parsedForm.FunctionCaseDefn;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.parsedForm.HandlerImplements;
import org.flasck.flas.parsedForm.HandlerLambda;
import org.flasck.flas.parsedForm.ImplementsContract;
import org.flasck.flas.parsedForm.IntroduceVar;
import org.flasck.flas.parsedForm.MakeAcor;
import org.flasck.flas.parsedForm.MakeSend;
import org.flasck.flas.parsedForm.Messages;
import org.flasck.flas.parsedForm.ObjectAccessor;
import org.flasck.flas.parsedForm.ObjectDefn;
import org.flasck.flas.parsedForm.ObjectMethod;
import org.flasck.flas.parsedForm.Provides;
import org.flasck.flas.parsedForm.RequiresContract;
import org.flasck.flas.parsedForm.SendMessage;
import org.flasck.flas.parsedForm.StandaloneMethod;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.StructField;
import org.flasck.flas.parsedForm.TupleAssignment;
import org.flasck.flas.parsedForm.TupleMember;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.parsedForm.TypedPattern;
import org.flasck.flas.parsedForm.UnionTypeDefn;
import org.flasck.flas.parsedForm.UnresolvedOperator;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.parsedForm.VarPattern;
import org.flasck.flas.parsedForm.ut.UnitTestAssert;
import org.flasck.flas.parsedForm.ut.UnitTestCase;
import org.flasck.flas.parsedForm.ut.UnitTestExpect;
import org.flasck.flas.parsedForm.ut.UnitTestInvoke;
import org.flasck.flas.parsedForm.ut.UnitTestPackage;
import org.flasck.flas.parsedForm.ut.UnitTestSend;
import org.flasck.flas.parsedForm.ut.UnitTestStep;
import org.flasck.flas.parser.ut.UnitDataDeclaration;
import org.flasck.flas.parser.ut.UnitDataDeclaration.Assignment;
import org.flasck.flas.repository.Repository.Visitor;
import org.flasck.flas.tc3.NamedType;
import org.flasck.flas.tc3.Primitive;
import org.flasck.flas.tc3.Type;

public class StackVisitor implements NestedVisitor, HSIVisitor, TreeOrderVisitor {
	private List<Visitor> stack = new LinkedList<>();
	private Visitor top;
	private HSIVisitor hsi;
	private TreeOrderVisitor tov;
	
	@Override
	public void push(Visitor v) {
//		System.out.println("Pushing " + v.getClass().getName());
		stack.add(0, v);
		setTop(v);
	}

	private void setTop(Visitor v) {
		this.top = v;
		if (v instanceof HSIVisitor)
			this.hsi = (HSIVisitor) v;
		else
			this.hsi = null;
		if (v instanceof TreeOrderVisitor)
			this.tov = (TreeOrderVisitor) v;
		else
			this.tov = null;
	}

	@Override
	public void result(Object r) {
//		System.out.println("Returning " + r + " from " + stack.get(0).getClass().getName());
		stack.remove(0);
		setTop(stack.get(0));
		if (this.top instanceof ResultAware)
			((ResultAware)this.top).result(r);
	}

	public void visitEntry(RepositoryEntry entry) {
		top.visitEntry(entry);
	}

	public void visitPrimitive(Primitive p) {
		top.visitPrimitive(p);
	}

	public void visitStructDefn(StructDefn s) {
		top.visitStructDefn(s);
	}

	public void visitStructField(StructField sf) {
		top.visitStructField(sf);
	}

	public void leaveStructField(StructField sf) {
		top.leaveStructField(sf);
	}

	public void leaveStructDefn(StructDefn s) {
		top.leaveStructDefn(s);
	}

	public void visitStructFieldAccessor(StructField sf) {
		top.visitStructFieldAccessor(sf);
	}

	public void leaveStructFieldAccessor(StructField sf) {
		top.leaveStructFieldAccessor(sf);
	}

	public void visitUnresolvedVar(UnresolvedVar var, int nargs) {
		top.visitUnresolvedVar(var, nargs);
	}

	public void visitUnionTypeDefn(UnionTypeDefn ud) {
		top.visitUnionTypeDefn(ud);
	}

	public void leaveUnionTypeDefn(UnionTypeDefn ud) {
		top.leaveUnionTypeDefn(ud);
	}

	public void visitUnresolvedOperator(UnresolvedOperator operator, int nargs) {
		top.visitUnresolvedOperator(operator, nargs);
	}

	@Override
	public void visitIntroduceVar(IntroduceVar var) {
		top.visitIntroduceVar(var);
	}

	@Override
	public void visitAnonymousVar(AnonymousVar var) {
		top.visitAnonymousVar(var);
	}

	public void visitTypeReference(TypeReference var) {
		top.visitTypeReference(var);
	}

	public void visitFunctionGroup(FunctionGroup grp) {
		top.visitFunctionGroup(grp);
	}

	public void visitFunction(FunctionDefinition fn) {
		top.visitFunction(fn);
	}

	public void visitFunctionIntro(FunctionIntro fi) {
		top.visitFunctionIntro(fi);
	}

	public void leaveFunctionIntro(FunctionIntro fi) {
		top.leaveFunctionIntro(fi);
	}

	public void leaveFunction(FunctionDefinition fn) {
		top.leaveFunction(fn);
	}

	public void leaveFunctionGroup(FunctionGroup grp) {
		top.leaveFunctionGroup(grp);
	}

	public void visitTuple(TupleAssignment e) {
		top.visitTuple(e);
	}

	public void tupleExprComplete(TupleAssignment e) {
		top.tupleExprComplete(e);
	}

	public void leaveTuple(TupleAssignment e) {
		top.leaveTuple(e);
	}

	@Override
	public void visitTupleMember(TupleMember sd) {
		top.visitTupleMember(sd);
	}

	@Override
	public void leaveTupleMember(TupleMember sd) {
		top.leaveTupleMember(sd);
	}

	public void visitPattern(Pattern patt, boolean isNested) {
		top.visitPattern(patt, isNested);
	}

	public void visitVarPattern(VarPattern p, boolean isNested) {
		top.visitVarPattern(p, isNested);
	}

	public void visitTypedPattern(TypedPattern p, boolean isNested) {
		top.visitTypedPattern(p, isNested);
	}

	public void visitConstructorMatch(ConstructorMatch p, boolean isNested) {
		top.visitConstructorMatch(p, isNested);
	}

	public void visitConstructorField(String field, Pattern patt, boolean isNested) {
		top.visitConstructorField(field, patt, isNested);
	}

	public void leaveConstructorField(String field, Object patt) {
		top.leaveConstructorField(field, patt);
	}

	public void leaveConstructorMatch(ConstructorMatch p) {
		top.leaveConstructorMatch(p);
	}

	public void visitPatternVar(InputPosition varLoc, String var) {
		top.visitPatternVar(varLoc, var);
	}

	public void visitConstPattern(ConstPattern p, boolean isNested) {
		top.visitConstPattern(p, isNested);
	}

	public void leavePattern(Object patt, boolean isNested) {
		top.leavePattern(patt, isNested);
	}

	public void visitCase(FunctionCaseDefn c) {
		top.visitCase(c);
	}

	public void visitGuard(FunctionCaseDefn c) {
		top.visitGuard(c);
	}

	public void leaveGuard(FunctionCaseDefn c) {
		top.leaveGuard(c);
	}

	public void leaveCase(FunctionCaseDefn c) {
		top.leaveCase(c);
	}

	public void startInline(FunctionIntro fi) {
		top.startInline(fi);
	}

	public void endInline(FunctionIntro fi) {
		top.endInline(fi);
	}

	public void visitExpr(Expr expr, int nArgs) {
		top.visitExpr(expr, nArgs);
	}

	public void visitStringLiteral(StringLiteral expr) {
		top.visitStringLiteral(expr);
	}

	public void visitNumericLiteral(NumericLiteral number) {
		top.visitNumericLiteral(number);
	}

	public void visitMakeSend(MakeSend expr) {
		top.visitMakeSend(expr);
	}

	public void leaveMakeSend(MakeSend expr) {
		top.leaveMakeSend(expr);
	}

	public void visitMakeAcor(MakeAcor expr) {
		top.visitMakeAcor(expr);
	}
	
	public void leaveMakeAcor(MakeAcor expr) {
		top.leaveMakeAcor(expr);
	}

	public void visitUnitTestPackage(UnitTestPackage e) {
		top.visitUnitTestPackage(e);
	}

	public void visitUnitTest(UnitTestCase e) {
		top.visitUnitTest(e);
	}

	public void leaveUnitTest(UnitTestCase e) {
		top.leaveUnitTest(e);
	}

	public void leaveUnitTestPackage(UnitTestPackage e) {
		top.leaveUnitTestPackage(e);
	}

	public void visitApplyExpr(ApplyExpr expr) {
		top.visitApplyExpr(expr);
	}

	public void leaveApplyExpr(ApplyExpr expr) {
		top.leaveApplyExpr(expr);
	}

	public void visitMemberExpr(MemberExpr expr) {
		top.visitMemberExpr(expr);
	}

	public void leaveMemberExpr(MemberExpr expr) {
		top.leaveMemberExpr(expr);
	}

	public void visitCurrentContainer(CurrentContainer expr) {
		top.visitCurrentContainer(expr);
	}

	public void visitUnitDataDeclaration(UnitDataDeclaration udd) {
		top.visitUnitDataDeclaration(udd);
	}

	public void leaveUnitDataDeclaration(UnitDataDeclaration udd) {
		top.leaveUnitDataDeclaration(udd);
	}

	public void visitUnitDataField(Assignment assign) {
		top.visitUnitDataField(assign);
	}

	public void leaveUnitDataField(Assignment assign) {
		top.leaveUnitDataField(assign);
	}

	public void visitUnitTestStep(UnitTestStep s) {
		top.visitUnitTestStep(s);
	}

	public void visitUnitTestAssert(UnitTestAssert a) {
		top.visitUnitTestAssert(a);
	}

	public void visitAssertExpr(boolean isValue, Expr e) {
		top.visitAssertExpr(isValue, e);
	}

	public void leaveAssertExpr(boolean isValue, Expr e) {
		top.leaveAssertExpr(isValue, e);
	}

	public void postUnitTestAssert(UnitTestAssert a) {
		top.postUnitTestAssert(a);
	}

	public void visitUnitTestInvoke(UnitTestInvoke uti) {
		top.visitUnitTestInvoke(uti);
	}

	public void leaveUnitTestInvoke(UnitTestInvoke uti) {
		top.leaveUnitTestInvoke(uti);
	}

	public void visitUnitTestExpect(UnitTestExpect s) {
		top.visitUnitTestExpect(s);
	}

	public void expectHandlerNext() {
		top.expectHandlerNext();
	}

	public void leaveUnitTestExpect(UnitTestExpect ute) {
		top.leaveUnitTestExpect(ute);
	}

	public void visitUnitTestSend(UnitTestSend s) {
		top.visitUnitTestSend(s);
	}

	@Override
	public void visitSendMethod(NamedType defn, UnresolvedVar expr) {
		top.visitSendMethod(defn, expr);
	}

	public void leaveUnitTestSend(UnitTestSend s) {
		top.leaveUnitTestSend(s);
	}

	public void visitContractDecl(ContractDecl cd) {
		top.visitContractDecl(cd);
	}

	public void visitContractMethod(ContractMethodDecl cmd) {
		top.visitContractMethod(cmd);
	}

	public void leaveContractMethod(ContractMethodDecl cmd) {
		top.leaveContractMethod(cmd);
	}

	public void leaveContractDecl(ContractDecl cd) {
		top.leaveContractDecl(cd);
	}

	public void visitObjectDefn(ObjectDefn e) {
		top.visitObjectDefn(e);
	}

	public void leaveObjectDefn(ObjectDefn obj) {
		top.leaveObjectDefn(obj);
	}

	public void visitObjectAccessor(ObjectAccessor oa) {
		top.visitObjectAccessor(oa);
	}

	public void visitAgentDefn(AgentDefinition s) {
		top.visitAgentDefn(s);
	}

	public void visitProvides(Provides p) {
		top.visitProvides(p);
	}

	public void leaveProvides(Provides p) {
		top.leaveProvides(p);
	}

	public void visitRequires(RequiresContract rc) {
		top.visitRequires(rc);
	}

	@Override
	public void visitImplements(ImplementsContract ic) {
		top.visitImplements(ic);
	}

	public void leaveImplements(ImplementsContract ic) {
		top.leaveImplements(ic);
	}

	public void visitHandlerImplements(HandlerImplements hi) {
		top.visitHandlerImplements(hi);
	}

	public void visitHandlerLambda(HandlerLambda hl) {
		top.visitHandlerLambda(hl);
	}

	public void leaveHandlerImplements(HandlerImplements hi) {
		top.leaveHandlerImplements(hi);
	}

	public void leaveAgentDefn(AgentDefinition s) {
		top.leaveAgentDefn(s);
	}

	public void leaveObjectAccessor(ObjectAccessor oa) {
		top.leaveObjectAccessor(oa);
	}

	public void visitObjectMethod(ObjectMethod e) {
		top.visitObjectMethod(e);
	}

	public void visitStandaloneMethod(StandaloneMethod meth) {
		top.visitStandaloneMethod(meth);
	}

	public void visitMessages(Messages messages) {
		top.visitMessages(messages);
	}

	public void leaveMessages(Messages msgs) {
		top.leaveMessages(msgs);
	}

	public void visitMessage(ActionMessage msg) {
		top.visitMessage(msg);
	}

	public void visitAssignMessage(AssignMessage msg) {
		top.visitAssignMessage(msg);
	}

	public void visitAssignSlot(List<UnresolvedVar> slot) {
		top.visitAssignSlot(slot);
	}

	public void leaveAssignMessage(AssignMessage msg) {
		top.leaveAssignMessage(msg);
	}

	public void visitSendMessage(SendMessage msg) {
		top.visitSendMessage(msg);
	}

	public void leaveSendMessage(SendMessage msg) {
		top.leaveSendMessage(msg);
	}

	public void leaveMessage(ActionMessage msg) {
		top.leaveMessage(msg);
	}

	public void leaveObjectMethod(ObjectMethod meth) {
		top.leaveObjectMethod(meth);
	}

	public void leaveStandaloneMethod(StandaloneMethod meth) {
		top.leaveStandaloneMethod(meth);
	}

	public void visitHandleExpr(InputPosition location, Expr expr, Expr handler) {
		top.visitHandleExpr(location, expr, handler);
	}

	public void leaveHandleExpr(Expr expr, Expr handler) {
		top.leaveHandleExpr(expr, handler);
	}

	public void traversalDone() {
		top.traversalDone();
	}

	public void hsiArgs(List<Slot> slots) {
		hsi.hsiArgs(slots);
	}

	public void switchOn(Slot slot) {
		hsi.switchOn(slot);
	}

	public void withConstructor(String string) {
		hsi.withConstructor(string);
	}

	public void constructorField(Slot parent, String field, Slot slot) {
		hsi.constructorField(parent, field, slot);
	}

	public void matchNumber(int i) {
		hsi.matchNumber(i);
	}

	public void matchString(String s) {
		hsi.matchString(s);
	}

	public void matchDefault() {
		hsi.matchDefault();
	}

	public void defaultCase() {
		hsi.defaultCase();
	}

	public void errorNoCase() {
		hsi.errorNoCase();
	}

	public void bind(Slot slot, String var) {
		hsi.bind(slot, var);
	}

	public void endSwitch() {
		hsi.endSwitch();
	}

	public void argSlot(Slot s) {
		tov.argSlot(s);
	}

	public void matchConstructor(StructDefn ctor) {
		tov.matchConstructor(ctor);
	}

	public void matchField(StructField fld) {
		tov.matchField(fld);
	}

	public void matchType(Type ty, VarName var, FunctionIntro intro) {
		tov.matchType(ty, var, intro);
	}

	public void varInIntro(VarName vn, VarPattern vp, FunctionIntro intro) {
		tov.varInIntro(vn, vp, intro);
	}

	public void endField(StructField fld) {
		tov.endField(fld);
	}

	public void endConstructor(StructDefn ctor) {
		tov.endConstructor(ctor);
	}

	public void endArg(Slot s) {
		tov.endArg(s);
	}
	
	@Override
	public String toString() {
		return "StackVisitor" + stack;
	}
}
