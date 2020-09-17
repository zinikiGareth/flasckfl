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
import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.commonBase.names.VarName;
import org.flasck.flas.hsi.ArgSlot;
import org.flasck.flas.hsi.HSIVisitor;
import org.flasck.flas.hsi.Slot;
import org.flasck.flas.hsi.TreeOrderVisitor;
import org.flasck.flas.parsedForm.ActionMessage;
import org.flasck.flas.parsedForm.AgentDefinition;
import org.flasck.flas.parsedForm.AnonymousVar;
import org.flasck.flas.parsedForm.AssignMessage;
import org.flasck.flas.parsedForm.CardDefinition;
import org.flasck.flas.parsedForm.CheckTypeExpr;
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
import org.flasck.flas.parsedForm.LogicHolder;
import org.flasck.flas.parsedForm.MakeAcor;
import org.flasck.flas.parsedForm.MakeSend;
import org.flasck.flas.parsedForm.Messages;
import org.flasck.flas.parsedForm.ObjectAccessor;
import org.flasck.flas.parsedForm.ObjectContract;
import org.flasck.flas.parsedForm.ObjectCtor;
import org.flasck.flas.parsedForm.ObjectDefn;
import org.flasck.flas.parsedForm.ObjectMethod;
import org.flasck.flas.parsedForm.Provides;
import org.flasck.flas.parsedForm.RequiresContract;
import org.flasck.flas.parsedForm.SendMessage;
import org.flasck.flas.parsedForm.ServiceDefinition;
import org.flasck.flas.parsedForm.StandaloneMethod;
import org.flasck.flas.parsedForm.StateDefinition;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.StructField;
import org.flasck.flas.parsedForm.Template;
import org.flasck.flas.parsedForm.TemplateBinding;
import org.flasck.flas.parsedForm.TemplateBindingOption;
import org.flasck.flas.parsedForm.TemplateCustomization;
import org.flasck.flas.parsedForm.TemplateEvent;
import org.flasck.flas.parsedForm.TemplateStylingOption;
import org.flasck.flas.parsedForm.TupleAssignment;
import org.flasck.flas.parsedForm.TupleMember;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.parsedForm.TypedPattern;
import org.flasck.flas.parsedForm.UnionTypeDefn;
import org.flasck.flas.parsedForm.UnresolvedOperator;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.parsedForm.VarPattern;
import org.flasck.flas.parsedForm.st.SystemTest;
import org.flasck.flas.parsedForm.st.SystemTestStage;
import org.flasck.flas.parsedForm.ut.GuardedMessages;
import org.flasck.flas.parsedForm.ut.TestStepHolder;
import org.flasck.flas.parsedForm.ut.UnitTestAssert;
import org.flasck.flas.parsedForm.ut.UnitTestCase;
import org.flasck.flas.parsedForm.ut.UnitTestEvent;
import org.flasck.flas.parsedForm.ut.UnitTestExpect;
import org.flasck.flas.parsedForm.ut.UnitTestInvoke;
import org.flasck.flas.parsedForm.ut.UnitTestMatch;
import org.flasck.flas.parsedForm.ut.UnitTestNewDiv;
import org.flasck.flas.parsedForm.ut.UnitTestPackage;
import org.flasck.flas.parsedForm.ut.UnitTestRender;
import org.flasck.flas.parsedForm.ut.UnitTestSend;
import org.flasck.flas.parsedForm.ut.UnitTestShove;
import org.flasck.flas.parsedForm.ut.UnitTestStep;
import org.flasck.flas.parser.ut.UnitDataDeclaration;
import org.flasck.flas.parser.ut.UnitDataDeclaration.Assignment;
import org.flasck.flas.tc3.NamedType;
import org.flasck.flas.tc3.Primitive;
import org.flasck.flas.tc3.Type;
import org.zinutils.exceptions.InvalidUsageException;

public class StackVisitor implements NestedVisitor, HSIVisitor, TreeOrderVisitor {
	private List<RepositoryVisitor> stack = new LinkedList<>();
	private RepositoryVisitor top;
	private HSIVisitor hsi;
	private TreeOrderVisitor tov;
	
	@Override
	public void push(RepositoryVisitor v) {
//		System.out.println("Pushing " + v.getClass().getName());
		stack.add(0, v);
		setTop(v);
	}

	private void setTop(RepositoryVisitor v) {
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

	public void visitTypeReference(TypeReference var, boolean expectPolys) {
		top.visitTypeReference(var, true);
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

	public void visitCheckTypeExpr(CheckTypeExpr expr) {
		top.visitCheckTypeExpr(expr);
	}

	public void leaveCheckTypeExpr(CheckTypeExpr expr) {
		top.leaveCheckTypeExpr(expr);
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

	public void leaveUnitTest(TestStepHolder e) {
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

	public boolean visitMemberExpr(MemberExpr expr, int nargs) {
		return top.visitMemberExpr(expr, nargs);
	}

	public void leaveMemberExpr(MemberExpr expr) {
		top.leaveMemberExpr(expr);
	}

	public void visitConvertedExpr(MemberExpr expr, int nargs) {
		top.visitConvertedExpr(expr, nargs);
	}

	public void leaveConvertedExpr(MemberExpr expr) {
		top.leaveConvertedExpr(expr);
	}

	public void visitCurrentContainer(CurrentContainer expr, boolean isObjState, boolean wouldWantState) {
		top.visitCurrentContainer(expr, isObjState, wouldWantState);
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

	public void visitUnitTestRender(UnitTestRender e) {
		top.visitUnitTestRender(e);
	}

	public void leaveUnitTestRender(UnitTestRender e) {
		top.leaveUnitTestRender(e);
	}

	public void visitUnitTestEvent(UnitTestEvent e) {
		top.visitUnitTestEvent(e);
	}

	public void leaveUnitTestEvent(UnitTestEvent e) {
		top.leaveUnitTestEvent(e);
	}

	public void visitUnitTestMatch(UnitTestMatch m) {
		top.visitUnitTestMatch(m);
	}

	public void leaveUnitTestMatch(UnitTestMatch m) {
		top.leaveUnitTestMatch(m);
	}

	public void visitUnitTestNewDiv(UnitTestNewDiv s) {
		top.visitUnitTestNewDiv(s);
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

	public void visitObjectContract(ObjectContract oc) {
		top.visitObjectContract(oc);
	}

	public void leaveObjectContract(ObjectContract oc) {
		top.leaveObjectContract(oc);
	}

	public void leaveObjectDefn(ObjectDefn obj) {
		top.leaveObjectDefn(obj);
	}

	public void visitObjectAccessor(ObjectAccessor oa) {
		top.visitObjectAccessor(oa);
	}

	public void visitObjectCtor(ObjectCtor oa) {
		top.visitObjectCtor(oa);
	}

	public void leaveObjectCtor(ObjectCtor oa) {
		top.leaveObjectCtor(oa);
	}

	@Override
	public void visitCardDefn(CardDefinition cd) {
		top.visitCardDefn(cd);
	}

	@Override
	public void leaveCardDefn(CardDefinition s) {
		top.leaveCardDefn(s);
	}

	@Override
	public void visitTemplate(Template t, boolean isFirst) {
		top.visitTemplate(t, isFirst);
	}

	public void afterTemplateChainTypes(Template t) {
		top.afterTemplateChainTypes(t);
	}

	public void visitTemplateBinding(TemplateBinding b) {
		top.visitTemplateBinding(b);
	}

	public void visitTemplateBindingOption(TemplateBindingOption option) {
		top.visitTemplateBindingOption(option);
	}

	public void visitTemplateBindingCondition(Expr cond) {
		top.visitTemplateBindingCondition(cond);
	}

	public void visitTemplateBindingExpr(Expr expr) {
		top.visitTemplateBindingExpr(expr);
	}

	public void visitTemplateStyleExpr(Expr e) {
		top.visitTemplateStyleExpr(e);
	}

	public void visitTemplateStyleCond(Expr cond) {
		top.visitTemplateStyleCond(cond);
	}

	public void leaveTemplateBindingOption(TemplateBindingOption option) {
		top.leaveTemplateBindingOption(option);
	}

	public void leaveTemplateBinding(TemplateBinding b) {
		top.leaveTemplateBinding(b);
	}

	public void visitTemplateCustomization(TemplateCustomization tc) {
		top.visitTemplateCustomization(tc);
	}

	public void leaveTemplateCustomization(TemplateCustomization tc) {
		top.leaveTemplateCustomization(tc);
	}

	public void visitTemplateStyling(TemplateStylingOption tso) {
		top.visitTemplateStyling(tso);
	}

	public void leaveTemplateStyling(TemplateStylingOption tso) {
		top.leaveTemplateStyling(tso);
	}

	public void visitTemplateEvent(TemplateEvent te) {
		top.visitTemplateEvent(te);
	}

	@Override
	public void leaveTemplate(Template t) {
		top.leaveTemplate(t);
	}

	public void visitAgentDefn(AgentDefinition s) {
		top.visitAgentDefn(s);
	}

	public void visitServiceDefn(ServiceDefinition s) {
		top.visitServiceDefn(s);
	}

	public void leaveServiceDefn(ServiceDefinition s) {
		top.leaveServiceDefn(s);
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

	public void visitEventSource(Template t) {
		top.visitEventSource(t);
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

	@Override
	public void visitGuardedMessage(GuardedMessages gm) {
		top.visitGuardedMessage(gm);
	}
	
	@Override
	public void leaveGuardedMessage(GuardedMessages gm) {
		top.leaveGuardedMessage(gm);
	}

	public void visitMessage(ActionMessage msg) {
		top.visitMessage(msg);
	}

	public void visitAssignMessage(AssignMessage msg) {
		top.visitAssignMessage(msg);
	}

	public void visitAssignSlot(Expr slot) {
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

	public void leaveStateDefinition(StateDefinition state) {
		top.leaveStateDefinition(state);
	}

	public void visitStateDefinition(StateDefinition state) {
		top.visitStateDefinition(state);
	}

	public void visitUnitTestShove(UnitTestShove s) {
		top.visitUnitTestShove(s);
	}

	public void visitShoveSlot(UnresolvedVar v) {
		top.visitShoveSlot(v);
	}

	public void visitShoveExpr(Expr value) {
		top.visitShoveExpr(value);
	}

	public void leaveUnitTestShove(UnitTestShove s) {
		top.leaveUnitTestShove(s);
	}

	public void visitSystemTest(SystemTest st) {
		top.visitSystemTest(st);
	}

	public void leaveSystemTest(SystemTest st) {
		top.leaveSystemTest(st);
	}

	public void visitSystemTestStage(SystemTestStage s) {
		top.visitSystemTestStage(s);
	}

	public void leaveSystemTestStage(SystemTestStage s) {
		top.leaveSystemTestStage(s);
	}

	public void traversalDone() {
		top.traversalDone();
		if (stack.size() > 1)
			throw new InvalidUsageException("Should end up with just top entry: " + stack);
	}

	public void hsiArgs(List<Slot> slots) {
		hsi.hsiArgs(slots);
	}

	public void switchOn(Slot slot) {
		hsi.switchOn(slot);
	}

	public void withConstructor(NameOfThing name) {
		hsi.withConstructor(name);
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

	public void argSlot(ArgSlot s) {
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
	
	public void patternsDone(LogicHolder fn) {
		tov.patternsDone(fn);
	}

	@Override
	public String toString() {
		return "StackVisitor" + stack;
	}

	public void reduceTo(int cnt) {
		while (stack.size() > cnt)
			stack.remove(0);
		setTop(stack.get(0));
	}
}
