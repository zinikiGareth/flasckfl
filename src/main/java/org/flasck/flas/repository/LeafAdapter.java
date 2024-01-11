package org.flasck.flas.repository;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.ApplyExpr;
import org.flasck.flas.commonBase.ConstPattern;
import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.commonBase.MemberExpr;
import org.flasck.flas.commonBase.NumericLiteral;
import org.flasck.flas.commonBase.Pattern;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.parsedForm.ActionMessage;
import org.flasck.flas.parsedForm.AgentDefinition;
import org.flasck.flas.parsedForm.AnonymousVar;
import org.flasck.flas.parsedForm.AssignMessage;
import org.flasck.flas.parsedForm.CardDefinition;
import org.flasck.flas.parsedForm.CastExpr;
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
import org.flasck.flas.parsedForm.TypeExpr;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.parsedForm.TypedPattern;
import org.flasck.flas.parsedForm.UnionTypeDefn;
import org.flasck.flas.parsedForm.UnresolvedOperator;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.parsedForm.VarPattern;
import org.flasck.flas.parsedForm.assembly.ApplicationAssembly;
import org.flasck.flas.parsedForm.assembly.ApplicationRouting;
import org.flasck.flas.parsedForm.assembly.ApplicationRouting.CardBinding;
import org.flasck.flas.parsedForm.assembly.RoutingAction;
import org.flasck.flas.parsedForm.assembly.RoutingActions;
import org.flasck.flas.parsedForm.assembly.SubRouting;
import org.flasck.flas.parsedForm.st.AjaxCreate;
import org.flasck.flas.parsedForm.st.AjaxPump;
import org.flasck.flas.parsedForm.st.AjaxSubscribe;
import org.flasck.flas.parsedForm.st.CreateMockApplication;
import org.flasck.flas.parsedForm.st.GotoRoute;
import org.flasck.flas.parsedForm.st.SystemTest;
import org.flasck.flas.parsedForm.st.SystemTestStage;
import org.flasck.flas.parsedForm.st.UserLogin;
import org.flasck.flas.parsedForm.ut.GuardedMessages;
import org.flasck.flas.parsedForm.ut.TestStepHolder;
import org.flasck.flas.parsedForm.ut.UnitTestAssert;
import org.flasck.flas.parsedForm.ut.UnitTestCase;
import org.flasck.flas.parsedForm.ut.UnitTestClose;
import org.flasck.flas.parsedForm.ut.UnitTestEvent;
import org.flasck.flas.parsedForm.ut.UnitTestExpect;
import org.flasck.flas.parsedForm.ut.UnitTestExpectCancel;
import org.flasck.flas.parsedForm.ut.UnitTestIdentical;
import org.flasck.flas.parsedForm.ut.UnitTestInput;
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

/** The purpose of the leaf adapter is to visit all of the leaves in the system.
 *
 *  This is not suitable for things that want to see (for example) 'apply' or 'dot' nodes in expressions, which should use the Pre or Post adapters 
 */
public class LeafAdapter implements RepositoryVisitor {
	@Override
	public void visitEntry(RepositoryEntry entry) {
	}

	@Override
	public void visitPrimitive(Primitive p) {
	}

	@Override
	public void visitStructDefn(StructDefn s) {
	}

	@Override
	public void leaveStructDefn(StructDefn s) {
	}

	@Override
	public void visitStructField(StructField sf) {
	}

	@Override
	public void leaveStructField(StructField sf) {
	}

	@Override
	public void visitStructFieldAccessor(StructField sf) {
	}

	@Override
	public void leaveStructFieldAccessor(StructField sf) {
	}

	@Override
	public void visitUnionTypeDefn(UnionTypeDefn ud) {
	}

	@Override
	public void leaveUnionTypeDefn(UnionTypeDefn ud) {
	}

	@Override
	public void visitObjectDefn(ObjectDefn obj) {
	}

	@Override
	public void visitObjectContract(ObjectContract oc) {
	}

	@Override
	public void leaveObjectContract(ObjectContract oc) {
	}

	@Override
	public void leaveObjectDefn(ObjectDefn obj) {
	}

	@Override
	public void visitCardDefn(CardDefinition cd) {
	}

	@Override
	public void leaveCardDefn(CardDefinition s) {
	}

	@Override
	public void visitAgentDefn(AgentDefinition s) {
	}

	@Override
	public void visitServiceDefn(ServiceDefinition s) {
	}

	@Override
	public void leaveServiceDefn(ServiceDefinition s) {
	}

	@Override
	public void visitTemplate(Template t, boolean isFirst) {
	}

	@Override
	public void afterTemplateChainTypes(Template t) {
	}

	@Override
	public void visitTemplateBinding(TemplateBinding b) {
	}

	@Override
	public void visitTemplateBindingOption(TemplateBindingOption option) {
	}

	@Override
	public void visitTemplateBindingCondition(Expr cond) {
	}
	
	@Override
	public void visitTemplateBindingExpr(Expr expr) {
	}

	@Override
	public void visitTemplateStyleExpr(Expr e) {
	}

	@Override
	public void visitTemplateStyleCond(Expr cond) {
	}

	@Override
	public void leaveTemplateBindingOption(TemplateBindingOption option) {
	}

	@Override
	public void leaveTemplateBinding(TemplateBinding b) {
	}

	@Override
	public void visitTemplateCustomization(TemplateCustomization tc) {
	}

	@Override
	public void leaveTemplateCustomization(TemplateCustomization tc) {
	}

	@Override
	public void visitTemplateStyling(TemplateStylingOption tso) {
	}

	@Override
	public void leaveTemplateStyling(TemplateStylingOption tso) {
	}

	@Override
	public void visitUnitTestRender(UnitTestRender e) {
	}

	@Override
	public void leaveUnitTestRender(UnitTestRender e) {
	}

	@Override
	public void visitTemplateEvent(TemplateEvent te) {
	}

	@Override
	public void leaveTemplate(Template t) {
	}

	@Override
	public void visitProvides(Provides p) {
	}

	@Override
	public void leaveProvides(Provides p) {
	}

	@Override
	public void visitRequires(RequiresContract rc) {
	}

	@Override
	public void visitImplements(ImplementsContract ic) {
	}

	@Override
	public void leaveImplements(ImplementsContract ic) {
	}

	@Override
	public void visitHandlerImplements(HandlerImplements hi) {
	}

	@Override
	public void visitHandlerLambda(HandlerLambda hl) {
	}

	@Override
	public void leaveHandlerImplements(HandlerImplements hi) {
	}

	@Override
	public void leaveAgentDefn(AgentDefinition s) {
	}

	@Override
	public void visitObjectAccessor(ObjectAccessor oa) {
	}

	@Override
	public void leaveObjectAccessor(ObjectAccessor oa) {
	}

	@Override
	public void visitObjectCtor(ObjectCtor oa) {
	}

	@Override
	public void leaveObjectCtor(ObjectCtor oa) {
	}

	@Override
	public void visitObjectMethod(ObjectMethod meth) {
	}

	@Override
	public void visitEventSource(Template t) {
	}

	@Override
	public void visitStandaloneMethod(StandaloneMethod meth) {
	}

	@Override
	public void visitGuardedMessage(GuardedMessages gm) {
	}

	@Override
	public void leaveGuardedMessage(GuardedMessages gm) {
	}

	@Override
	public void visitMessage(ActionMessage msg) {
	}

	@Override
	public void visitAssignMessage(AssignMessage msg) {
	}

	@Override
	public void visitAssignSlot(Expr slot) {
	}

	@Override
	public void leaveAssignMessage(AssignMessage msg) {
	}

	@Override
	public void visitSendMessage(SendMessage msg) {
	}

	@Override
	public void visitHandlerName(Expr handlerName) {
	}

	@Override
	public void leaveHandlerName(Expr handlerName) {
	}

	@Override
	public void leaveSendMessage(SendMessage msg) {
	}

	@Override
	public void visitMessages(Messages messages) {
	}

	@Override
	public void leaveMessages(Messages msgs) {
	}

	@Override
	public void leaveMessage(ActionMessage msg) {
	}

	@Override
	public void leaveObjectMethod(ObjectMethod meth) {
	}

	@Override
	public void leaveStandaloneMethod(StandaloneMethod meth) {
	}

	@Override
	public void visitFunctionGroup(FunctionGroup grp) {
	}
	
	@Override
	public void visitFunction(FunctionDefinition fn) {
	}

	@Override
	public void visitFunctionIntro(FunctionIntro fi) {
	}

	@Override
	public void leaveFunctionIntro(FunctionIntro fi) {
	}

	@Override
	public void leaveFunction(FunctionDefinition fn) {
	}

	@Override
	public void leaveFunctionGroup(FunctionGroup grp) {
	}

	@Override
	public void visitTuple(TupleAssignment e) {
	}

	@Override
	public void tupleExprComplete(TupleAssignment e) {
	}

	@Override
	public void leaveTuple(TupleAssignment e) {
	}

	@Override
	public void visitTupleMember(TupleMember sd) {
	}

	@Override
	public void leaveTupleMember(TupleMember sd) {
	}

	@Override
	public void visitPattern(Pattern patt, boolean isNested) {
	}

	@Override
	public void visitVarPattern(VarPattern p, boolean isNested) {
	}

	@Override
	public void visitTypedPattern(TypedPattern p, boolean isNested) {
	}

	@Override
	public void visitConstructorMatch(ConstructorMatch p, boolean isNested) {
	}

	@Override
	public void visitConstructorField(String field, Pattern patt, boolean isNested) {
	}

	@Override
	public void leaveConstructorField(String field, Object patt) {
	}

	@Override
	public void leaveConstructorMatch(ConstructorMatch p) {
	}

	@Override
	public void visitConstPattern(ConstPattern p, boolean isNested) {
	}

	@Override
	public void visitPatternVar(InputPosition varLoc, String var) {
	}

	@Override
	public void leavePattern(Object patt, boolean isNested) {
	}

	@Override
	public void visitCase(FunctionCaseDefn c) {
	}

	@Override
	public void startInline(FunctionIntro fi) {
	}

	@Override
	public void visitGuard(FunctionCaseDefn c) {
	}

	@Override
	public void leaveGuard(FunctionCaseDefn c) {
	}

	@Override
	public void leaveCase(FunctionCaseDefn c) {
	}

	@Override
	public void endInline(FunctionIntro fi) {
	}

	@Override
	public void visitExpr(Expr expr, int nArgs) {
	}

	@Override
	public void visitApplyExpr(ApplyExpr expr) {
	}

	@Override
	public void leaveApplyExpr(ApplyExpr expr) {
	}

	@Override
	public boolean visitMemberExpr(MemberExpr expr, int nargs) {
		return expr.boundEarly();
	}

	@Override
	public void leaveMemberExpr(MemberExpr expr) {
	}

	@Override
	public void visitConvertedExpr(MemberExpr expr, int nargs) {
	}

	@Override
	public void leaveConvertedExpr(MemberExpr expr) {
	}

	@Override
	public void visitCheckTypeExpr(CheckTypeExpr expr) {
	}

	@Override
	public void leaveCheckTypeExpr(CheckTypeExpr expr) {
	}

	@Override
	public void visitStringLiteral(StringLiteral expr) {
	}

	@Override
	public void visitNumericLiteral(NumericLiteral number) {
	}

	@Override
	public void visitUnresolvedVar(UnresolvedVar var, int nargs) {
	}

	@Override
	public void visitUnresolvedOperator(UnresolvedOperator operator, int nargs) {
	}

	@Override
	public void visitIntroduceVar(IntroduceVar var) {
	}

	@Override
	public void visitAnonymousVar(AnonymousVar var) {
	}

	@Override
	public void visitTypeReference(TypeReference var, boolean expectPolys, int exprNargs) {
	}

	@Override
	public void visitMakeSend(MakeSend expr) {
	}

	@Override
	public void leaveMakeSend(MakeSend expr) {
	}

	@Override
	public void visitMakeAcor(MakeAcor expr) {
	}
	
	@Override
	public void leaveMakeAcor(MakeAcor expr) {
	}

	@Override
	public void visitCurrentContainer(CurrentContainer expr, boolean isObjState, boolean wouldWantState) {
	}

	@Override
	public void visitUnitTestPackage(UnitTestPackage e) {
	}

	@Override
	public void visitUnitTest(UnitTestCase e) {
	}

	@Override
	public void leaveUnitTest(TestStepHolder e) {
	}

	@Override
	public void leaveUnitTestPackage(UnitTestPackage e) {
	}
	
	@Override
	public void visitUnitDataDeclaration(UnitDataDeclaration udd) {
	}

	@Override
	public void leaveUnitDataDeclaration(UnitDataDeclaration udd) {
	}

	@Override
	public void visitUnitDataField(Assignment assign) {
	}

	@Override
	public void leaveUnitDataField(Assignment assign) {
	}

	@Override
	public void visitUnitTestAssert(UnitTestAssert a) {
	}

	@Override
	public void visitUnitTestStep(UnitTestStep s) {
	}

	@Override
	public void leaveUnitTestStep(UnitTestStep s) {
	}

	@Override
	public void visitAssertExpr(boolean isValue, Expr e) {
	}

	@Override
	public void leaveAssertExpr(boolean isValue, Expr e) {
	}

	@Override
	public void postUnitTestAssert(UnitTestAssert a) {
	}
	
	@Override
	public void visitUnitTestIdentical(UnitTestIdentical a) {
	}

	@Override
	public void postUnitTestIdentical(UnitTestIdentical a) {
	}

	@Override
	public void visitUnitTestInvoke(UnitTestInvoke uti) {
	}

	@Override
	public void leaveUnitTestInvoke(UnitTestInvoke uti) {
	}

	@Override
	public void visitUnitTestExpect(UnitTestExpect s) {
	}

	@Override
	public void visitUnitTestExpectCancel(UnitTestExpectCancel s) {
	}

	@Override
	public void leaveUnitTestExpectCancel(UnitTestExpectCancel utec) {
	}

	@Override
	public void expectHandlerNext() {
	}

	@Override
	public void leaveUnitTestExpect(UnitTestExpect ute) {
	}

	@Override
	public void visitUnitTestSend(UnitTestSend s) {
	}

	@Override
	public void visitSendMethod(NamedType defn, UnresolvedVar expr) {
	}

	@Override
	public void leaveUnitTestSend(UnitTestSend s) {
	}

	@Override
	public void visitUnitTestEvent(UnitTestEvent e) {
	}

	@Override
	public void leaveUnitTestEvent(UnitTestEvent e) {
	}

	@Override
	public void visitUnitTestInput(UnitTestInput e) {
	}

	@Override
	public void leaveUnitTestInput(UnitTestInput e) {
	}

	@Override
	public void visitUnitTestMatch(UnitTestMatch m) {
	}

	@Override
	public void leaveUnitTestMatch(UnitTestMatch m) {
	}

	@Override
	public void visitUnitTestNewDiv(UnitTestNewDiv s) {
	}

	@Override
	public void visitContractDecl(ContractDecl cd) {
	}

	@Override
	public void visitContractMethod(ContractMethodDecl cmd) {
	}

	@Override
	public void leaveContractMethod(ContractMethodDecl cmd) {
	}

	@Override
	public void leaveContractDecl(ContractDecl cd) {
	}

	@Override
	public void visitHandleExpr(InputPosition location, Expr expr, Expr handler) {
	}

	@Override
	public void leaveHandleExpr(Expr expr, Expr handler) {
	}

	@Override
	public void leaveStateDefinition(StateDefinition state) {
	}

	@Override
	public void visitStateDefinition(StateDefinition state) {
	}

	@Override
	public void visitUnitTestShove(UnitTestShove s) {
	}

	@Override
	public void visitShoveSlot(UnresolvedVar v) {
	}

	@Override
	public void visitShoveExpr(Expr value) {
	}

	@Override
	public void leaveUnitTestShove(UnitTestShove s) {
	}

	@Override
	public void visitSystemTest(SystemTest st) {
	}

	@Override
	public void leaveSystemTest(SystemTest st) {
	}

	@Override
	public void visitSystemTestStage(SystemTestStage s) {
	}

	@Override
	public void leaveSystemTestStage(SystemTestStage s) {
	}

	@Override
	public void visitAjaxCreate(AjaxCreate ac) {
	}

	@Override
	public void leaveAjaxCreate(AjaxCreate ac) {
	}

	@Override
	public void visitAjaxExpectSubscribe(AjaxSubscribe as) {
	}

	@Override
	public void leaveAjaxExpectSubscribe(AjaxSubscribe as) {
	}

	@Override
	public void visitAjaxPump(AjaxPump ac) {
	}

	@Override
	public void visitMockApplication(CreateMockApplication s) {
	}

	@Override
	public void visitGotoRoute(GotoRoute gr) {
	}

	@Override
	public void leaveGotoRoute(GotoRoute gr) {
	}

	@Override
	public void visitUserLogin(UserLogin ul) {
	}

	@Override
	public void leaveUserLogin(UserLogin ul) {
	}

	@Override
	public void visitTypeExpr(TypeExpr expr) {
	}

	@Override
	public void leaveTypeExpr(TypeExpr expr) {
	}

	@Override
	public void visitCastExpr(CastExpr expr) {
	}

	@Override
	public void leaveCastExpr(CastExpr expr) {
	}

	@Override
	public void visitAssembly(ApplicationAssembly e) {
	}

	@Override
	public void leaveAssembly(ApplicationAssembly e) {
	}

	@Override
	public void visitApplicationRouting(ApplicationRouting e) {
	}

	@Override
	public void leaveApplicationRouting(ApplicationRouting e) {
	}

	@Override
	public void visitSubRouting(SubRouting r) {
	}

	@Override
	public void leaveSubRouting(SubRouting r) {
	}

	@Override
	public void visitCardAssignment(CardBinding card) {
	}

	@Override
	public void leaveCardAssignment(CardBinding card) {
	}

	@Override
	public void visitActions(RoutingActions actions) {
	}

	@Override
	public void leaveActions(RoutingActions actions) {
	}

	@Override
	public void visitRoutingAction(RoutingAction a) {
	}

	@Override
	public void visitRoutingExpr(RoutingAction a, int pos, Expr e) {
	}

	@Override
	public void leaveRoutingExpr(RoutingAction a, int pos, Expr e) {
	}

	@Override
	public void leaveRoutingAction(RoutingAction a) {
	}

	@Override
	public void visitUnitTestClose(UnitTestClose s) {
	}

	@Override
	public void leaveUnitTestClose(UnitTestClose s) {
	}

	@Override
	public void traversalDone() {
	}
}
