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
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.parsedForm.TypedPattern;
import org.flasck.flas.parsedForm.UnionTypeDefn;
import org.flasck.flas.parsedForm.UnresolvedOperator;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.parsedForm.VarPattern;
import org.flasck.flas.parsedForm.st.AjaxCreate;
import org.flasck.flas.parsedForm.st.AjaxPump;
import org.flasck.flas.parsedForm.st.AjaxSubscribe;
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

public interface RepositoryVisitor {
	void visitEntry(RepositoryEntry entry);
	void visitPrimitive(Primitive p);
	void visitStructDefn(StructDefn s);
	void visitStructField(StructField sf);
	void leaveStructField(StructField sf);
	void leaveStructDefn(StructDefn s);
	void visitStructFieldAccessor(StructField sf);
	void leaveStructFieldAccessor(StructField sf);
	void visitUnionTypeDefn(UnionTypeDefn ud);
	void leaveUnionTypeDefn(UnionTypeDefn ud);
	void visitUnresolvedVar(UnresolvedVar var, int nargs);
	void visitUnresolvedOperator(UnresolvedOperator operator, int nargs);
	void visitAnonymousVar(AnonymousVar var);
	void visitIntroduceVar(IntroduceVar var);
	void visitTypeReference(TypeReference var, boolean expectPolys);
	void visitFunctionGroup(FunctionGroup grp);
	void visitFunction(FunctionDefinition fn);
	void visitFunctionIntro(FunctionIntro fi);
	void leaveFunctionIntro(FunctionIntro fi);
	void leaveFunction(FunctionDefinition fn);
	void leaveFunctionGroup(FunctionGroup grp);
	void visitTuple(TupleAssignment e);
	void tupleExprComplete(TupleAssignment e);
	void visitTupleMember(TupleMember sd);
	void leaveTupleMember(TupleMember sd);
	void leaveTuple(TupleAssignment e);
	void visitPattern(Pattern patt, boolean isNested);
	void visitVarPattern(VarPattern p, boolean isNested);
	void visitTypedPattern(TypedPattern p, boolean isNested);
	void visitConstructorMatch(ConstructorMatch p, boolean isNested);
	void visitConstructorField(String field, Pattern patt, boolean isNested);
	void leaveConstructorField(String field, Object patt);
	void leaveConstructorMatch(ConstructorMatch p);
	void visitPatternVar(InputPosition varLoc, String var);
	void leavePattern(Object patt, boolean isNested);
	void startInline(FunctionIntro fi);
	void visitCase(FunctionCaseDefn c);
	void visitGuard(FunctionCaseDefn c);
	void leaveGuard(FunctionCaseDefn c);
	void leaveCase(FunctionCaseDefn c);
	void endInline(FunctionIntro fi);
	void visitExpr(Expr expr, int nArgs);
	void visitStringLiteral(StringLiteral expr);
	void visitNumericLiteral(NumericLiteral number);
	void visitUnitTestPackage(UnitTestPackage e);
	void visitUnitTest(UnitTestCase e);
	void leaveUnitTest(TestStepHolder e);
	void leaveUnitTestPackage(UnitTestPackage e);
	void visitApplyExpr(ApplyExpr expr);
	void leaveApplyExpr(ApplyExpr expr);
	void visitUnitTestStep(UnitTestStep s);
	void visitUnitTestAssert(UnitTestAssert a);
	void postUnitTestAssert(UnitTestAssert a);
	void visitContractDecl(ContractDecl cd);
	void visitContractMethod(ContractMethodDecl cmd);
	void leaveContractMethod(ContractMethodDecl cmd);
	void leaveContractDecl(ContractDecl cd);
	void visitObjectDefn(ObjectDefn obj);
	void leaveObjectDefn(ObjectDefn obj);
	void visitAgentDefn(AgentDefinition s);
	void visitProvides(Provides p);
	void leaveProvides(Provides p);
	void visitRequires(RequiresContract rc);
	void visitImplements(ImplementsContract ic);
	void leaveImplements(ImplementsContract ic);
	void visitHandlerImplements(HandlerImplements hi);
	void visitHandlerLambda(HandlerLambda hl);
	void leaveHandlerImplements(HandlerImplements hi);
	void leaveAgentDefn(AgentDefinition s);
	void visitObjectAccessor(ObjectAccessor oa);
	void leaveObjectAccessor(ObjectAccessor oa);
	void visitStandaloneMethod(StandaloneMethod meth);
	void visitObjectMethod(ObjectMethod meth);
	void visitMessages(Messages messages);
	void visitMessage(ActionMessage msg);
	void visitAssignMessage(AssignMessage msg);
	void visitAssignSlot(Expr slot);
	void leaveAssignMessage(AssignMessage msg);
	void visitSendMessage(SendMessage msg);
	void leaveSendMessage(SendMessage msg);
	void leaveMessage(ActionMessage msg);
	void leaveMessages(Messages msgs);
	void leaveObjectMethod(ObjectMethod meth);
	void leaveStandaloneMethod(StandaloneMethod meth);
	void visitMakeSend(MakeSend expr);
	void leaveMakeSend(MakeSend expr);
	void visitMakeAcor(MakeAcor expr);
	void leaveMakeAcor(MakeAcor expr);
	void visitCurrentContainer(CurrentContainer expr, boolean isObjState, boolean wouldWantState);
	void visitAssertExpr(boolean isValue, Expr e);
	void leaveAssertExpr(boolean isValue, Expr e);
	void visitConstPattern(ConstPattern p, boolean isNested);
	boolean visitMemberExpr(MemberExpr expr, int nargs);
	void leaveMemberExpr(MemberExpr expr);
	void visitUnitDataDeclaration(UnitDataDeclaration udd);
	void leaveUnitDataDeclaration(UnitDataDeclaration udd);
	void visitUnitDataField(Assignment assign);
	void leaveUnitDataField(Assignment assign);
	void visitUnitTestInvoke(UnitTestInvoke uti);
	void leaveUnitTestInvoke(UnitTestInvoke uti);
	void visitUnitTestExpect(UnitTestExpect s);
	void expectHandlerNext();
	void leaveUnitTestExpect(UnitTestExpect ute);
	void visitUnitTestSend(UnitTestSend s);
	void leaveUnitTestSend(UnitTestSend s);
	void visitUnitTestRender(UnitTestRender e);
	void visitUnitTestEvent(UnitTestEvent e);
	void leaveUnitTestEvent(UnitTestEvent e);
	void visitSendMethod(NamedType defn, UnresolvedVar expr);
	void visitHandleExpr(InputPosition location, Expr expr, Expr handler);
	void leaveHandleExpr(Expr expr, Expr handler);
	void traversalDone();
	void visitObjectContract(ObjectContract oc);
	void leaveObjectContract(ObjectContract oc);
	void visitObjectCtor(ObjectCtor oa);
	void leaveObjectCtor(ObjectCtor oa);
	void leaveStateDefinition(StateDefinition state);
	void visitStateDefinition(StateDefinition state);
	void visitServiceDefn(ServiceDefinition s);
	void leaveServiceDefn(ServiceDefinition s);
	void visitCardDefn(CardDefinition cd);
	void leaveCardDefn(CardDefinition s);
	void visitTemplate(Template t, boolean isFirst);
	void leaveTemplate(Template t);
	void visitUnitTestMatch(UnitTestMatch m);
	void leaveUnitTestMatch(UnitTestMatch m);
	void visitTemplateBinding(TemplateBinding b);
	void visitTemplateBindingOption(TemplateBindingOption option);
	void leaveTemplateBindingOption(TemplateBindingOption option);
	void leaveTemplateBinding(TemplateBinding b);
	void visitTemplateCustomization(TemplateCustomization tc);
	void leaveTemplateCustomization(TemplateCustomization tc);
	void visitTemplateStyling(TemplateStylingOption tso);
	void leaveTemplateStyling(TemplateStylingOption tso);
	void visitTemplateEvent(TemplateEvent te);
	void visitUnitTestShove(UnitTestShove s);
	void visitShoveSlot(UnresolvedVar v);
	void visitShoveExpr(Expr value);
	void leaveUnitTestShove(UnitTestShove s);
	void visitTemplateBindingCondition(Expr cond);
	void visitTemplateBindingExpr(Expr expr);
	void visitTemplateStyleExpr(Expr e);
	void visitTemplateStyleCond(Expr cond);
	void afterTemplateChainTypes(Template t);
	void visitEventSource(Template t);
	void visitUnitTestNewDiv(UnitTestNewDiv s);
	void visitGuardedMessage(GuardedMessages gm);
	void leaveGuardedMessage(GuardedMessages gm);
	void visitCheckTypeExpr(CheckTypeExpr expr);
	void leaveCheckTypeExpr(CheckTypeExpr expr);
	void visitConvertedExpr(MemberExpr expr, int nargs);
	void leaveConvertedExpr(MemberExpr expr);
	void leaveUnitTestRender(UnitTestRender e);
	void visitSystemTest(SystemTest st);
	void leaveSystemTest(SystemTest st);
	void visitSystemTestStage(SystemTestStage s);
	void leaveSystemTestStage(SystemTestStage s);
	void visitAjaxCreate(AjaxCreate ac);
	void leaveAjaxCreate(AjaxCreate ac);
	void visitAjaxExpectSubscribe(AjaxSubscribe as);
	void leaveAjaxExpectSubscribe(AjaxSubscribe as);
	void visitAjaxPump(AjaxPump ac);
}