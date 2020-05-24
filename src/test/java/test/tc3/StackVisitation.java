package test.tc3;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.ApplyExpr;
import org.flasck.flas.commonBase.MemberExpr;
import org.flasck.flas.commonBase.NumericLiteral;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.commonBase.names.SolidName;
import org.flasck.flas.commonBase.names.VarName;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.AnonymousVar;
import org.flasck.flas.parsedForm.AssignMessage;
import org.flasck.flas.parsedForm.ContractDecl;
import org.flasck.flas.parsedForm.ContractDecl.ContractType;
import org.flasck.flas.parsedForm.ContractMethodDecl;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.parsedForm.ObjectAccessor;
import org.flasck.flas.parsedForm.ObjectDefn;
import org.flasck.flas.parsedForm.ObjectMethod;
import org.flasck.flas.parsedForm.PolyType;
import org.flasck.flas.parsedForm.SendMessage;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.parsedForm.TypedPattern;
import org.flasck.flas.parsedForm.UnresolvedOperator;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.parsedForm.VarPattern;
import org.flasck.flas.patterns.HSIArgsTree;
import org.flasck.flas.repository.LoadBuiltins;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.repository.RepositoryReader;
import org.flasck.flas.tc3.Apply;
import org.flasck.flas.tc3.ApplyExpressionChecker;
import org.flasck.flas.tc3.CurrentTCState;
import org.flasck.flas.tc3.CurryArgumentType;
import org.flasck.flas.tc3.ErrorType;
import org.flasck.flas.tc3.ExpressionChecker;
import org.flasck.flas.tc3.ExpressionChecker.ExprResult;
import org.flasck.flas.tc3.FunctionChecker;
import org.flasck.flas.tc3.GroupChecker;
import org.flasck.flas.tc3.MemberExpressionChecker;
import org.flasck.flas.tc3.PosType;
import org.flasck.flas.tc3.SingleFunctionChecker;
import org.flasck.flas.tc3.Type;
import org.flasck.flas.tc3.TypeChecker;
import org.flasck.flas.tc3.TypeConstraintSet;
import org.flasck.flas.tc3.UnifiableType;
import org.hamcrest.Matchers;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.zinutils.support.jmock.CaptureAction;

import flas.matchers.ApplyMatcher;

public class StackVisitation {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	private InputPosition pos = new InputPosition("-", 1, 0, "hello");
	private final PackageName pkg = new PackageName("test.repo");
	private ErrorReporter errors = context.mock(ErrorReporter.class);
	private RepositoryReader repository = context.mock(RepositoryReader.class);
	private NestedVisitor nv = context.mock(NestedVisitor.class);
	private CurrentTCState state = context.mock(CurrentTCState.class);

	@Before
	public void begin() {
		context.checking(new Expectations() {{
			allowing(state).debugInfo(with(any(String.class)));
		}});
	}
	
	@Test
	public void whenWeVisitAFunctionWePushAFunctionChecker() {
		FunctionName name = FunctionName.function(pos, null, "f");
		FunctionIntro fi = new FunctionIntro(name, new ArrayList<>());
		fi.bindTree(new HSIArgsTree(0));
		context.checking(new Expectations() {{
			oneOf(nv).push(with(any(GroupChecker.class)));
			oneOf(nv).push(with(any(FunctionChecker.class)));
		}});
		GroupChecker gc = new GroupChecker(errors, repository, nv, null);
		gc.visitFunction(new FunctionDefinition(name, 0, false));
	}

	@Test
	public void whenWeVisitAnObjectMethodWePushAFunctionChecker() {
		SolidName obj = new SolidName(pkg, "MyObject");
		ObjectMethod meth = new ObjectMethod(pos, FunctionName.objectMethod(pos, obj, "meth"), new ArrayList<>(), null);
		meth.assignMessage(new AssignMessage(pos, null, new StringLiteral(pos, "hello")));
		context.checking(new Expectations() {{
			oneOf(nv).push(with(any(TypeChecker.class)));
			oneOf(nv).push(with(any(FunctionChecker.class)));
			oneOf(nv).push(with(any(SingleFunctionChecker.class)));
		}});
		TypeChecker gc = new TypeChecker(errors, repository, nv);
		gc.visitObjectMethod(meth);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void theResultIsWhatWeEndUpSpittingOut() {
		FunctionName name = FunctionName.function(pos, null, "f");
		FunctionDefinition fn = new FunctionDefinition(name, 0, false);
		FunctionIntro fi = new FunctionIntro(name, new ArrayList<>());
		fi.bindTree(new HSIArgsTree(0));
		fn.intro(fi);
		Type ty = context.mock(Type.class, "ty");
		UnifiableType utf = context.mock(UnifiableType.class, "utf");
		context.checking(new Expectations() {{
			oneOf(nv).push(with(any(GroupChecker.class)));
			oneOf(nv).push(with(any(FunctionChecker.class)));
			allowing(state).requireVarConstraints(pos, "f"); will(returnValue(utf));
			oneOf(state).groupDone(with(errors), with(any(Map.class)));
//			oneOf(utf).determinedType(new PosType(pos, ty));
//			oneOf(state).resolveAll(errors, false);
//			oneOf(state).enhanceAllMutualUTs();
//			oneOf(state).resolveAll(errors, true);
//			oneOf(state).bindVarPatternTypes(errors);
			oneOf(nv).result(null);
		}});
		GroupChecker gc = new GroupChecker(errors, repository, nv, state);
		gc.visitFunction(fn);
		gc.result(new PosType(pos, ty));
		gc.leaveFunctionIntro(fi);
		gc.leaveFunction(fn);
		gc.leaveFunctionGroup(null);
//		assertEquals(ty, fn.type());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void weCanTypecheckMethods() {
		FunctionName name = FunctionName.standaloneMethod(pos, null, "meth");
		ObjectMethod om = new ObjectMethod(pos, name, new ArrayList<>(), null);
		NumericLiteral e1 = new NumericLiteral(pos, "42", 2);
		om.sendMessage(new SendMessage(pos, e1));

		FunctionIntro fi = new FunctionIntro(name, new ArrayList<>());
		fi.bindTree(new HSIArgsTree(0));
		om.conversion(Arrays.asList(fi));
		
		Type ty = context.mock(Type.class);
		context.checking(new Expectations() {{
			oneOf(nv).push(with(any(GroupChecker.class)));
			oneOf(nv).push(with(any(FunctionChecker.class)));
		}});
		GroupChecker gc = new GroupChecker(errors, repository, nv, state);
		gc.visitObjectMethod(om);
		context.assertIsSatisfied();
		UnifiableType utm = context.mock(UnifiableType.class, "utm");
		context.checking(new Expectations() {{
			allowing(state).requireVarConstraints(pos, "meth"); will(returnValue(utm));
			oneOf(state).groupDone(with(errors), with(any(Map.class)));
//			oneOf(utm).determinedType(new PosType(pos, ty));
//			oneOf(state).resolveAll(errors, false);
//			oneOf(state).enhanceAllMutualUTs();
//			oneOf(state).resolveAll(errors, true);
//			oneOf(state).bindVarPatternTypes(errors);
			oneOf(nv).result(null);
		}});
		gc.result(new PosType(pos, ty));
		gc.leaveObjectMethod(om);
		gc.leaveFunctionGroup(null);
//		assertEquals(ty, om.type());
	}

	@Test
	public void weBindTheTypeAfterVisitingAnObjectMethod() {
		SolidName obj = new SolidName(pkg, "MyObject");
		ObjectMethod meth = new ObjectMethod(pos, FunctionName.objectMethod(pos, obj, "meth"), new ArrayList<>(), null);
		meth.assignMessage(new AssignMessage(pos, null, new StringLiteral(pos, "hello")));
		CaptureAction captureSFC = new CaptureAction(null);
		Type ty = context.mock(Type.class, "ty");
		context.checking(new Expectations() {{
			oneOf(nv).push(with(any(TypeChecker.class)));
			oneOf(nv).push(with(any(FunctionChecker.class)));
			oneOf(nv).push(with(any(SingleFunctionChecker.class))); will(captureSFC);
		}});
		TypeChecker gc = new TypeChecker(errors, repository, nv);
		gc.visitObjectMethod(meth);
		context.assertIsSatisfied();
		context.checking(new Expectations() {{
			oneOf(nv).result(null);
		}});
		SingleFunctionChecker sfc = (SingleFunctionChecker) captureSFC.get(0);
		sfc.result(new PosType(pos, ty));
		assertEquals(ty, meth.type());
	}

	@Test
	public void applyExpressionsPushAnotherMatcher() {
		ExpressionChecker ec = new ExpressionChecker(errors, repository, state, nv, false);
		context.checking(new Expectations() {{
			oneOf(nv).push(with(any(ApplyExpressionChecker.class)));
		}});
		UnresolvedOperator op = new UnresolvedOperator(pos, "+");
		NumericLiteral e1 = new NumericLiteral(pos, "42", 2);
		NumericLiteral e2 = new NumericLiteral(pos, "42", 2);
		ApplyExpr ae = new ApplyExpr(pos, op, e1, e2);
		ec.visitApplyExpr(ae);
	}

	@Test
	public void memberExpressionsPushAnotherMatcher() {
		ExpressionChecker ec = new ExpressionChecker(errors, repository, state, nv, false);
		context.checking(new Expectations() {{
			oneOf(nv).push(with(any(MemberExpressionChecker.class)));
		}});
		UnresolvedVar obj = new UnresolvedVar(pos, "obj");
		UnresolvedVar fld = new UnresolvedVar(pos, "f");
		MemberExpr ae = new MemberExpr(pos, obj, fld);
		ec.visitMemberExpr(ae);
	}

	@Test
	public void applyExpressionCheckerAutoPushesOnExpr() {
		ApplyExpressionChecker aec = new ApplyExpressionChecker(errors, repository, state, nv, false);
		context.checking(new Expectations() {{
			oneOf(nv).push(with(any(ExpressionChecker.class)));
		}});
		UnresolvedOperator op = new UnresolvedOperator(pos, "+");
		aec.visitExpr(op, 2);
	}

	@Test
	public void applyCheckerPushesOnMemberExpr() {
		ApplyExpressionChecker aec = new ApplyExpressionChecker(errors, repository, state, nv, false);
		context.checking(new Expectations() {{
			oneOf(nv).push(with(any(MemberExpressionChecker.class)));
		}});
		UnresolvedVar obj = new UnresolvedVar(pos, "obj");
		UnresolvedVar fld = new UnresolvedVar(pos, "f");
		MemberExpr ae = new MemberExpr(pos, obj, fld);
		aec.visitMemberExpr(ae);
	}

	@Test
	public void leaveApplyExpressionWithValidTypesReturnsResult() {
		ApplyExpressionChecker aec = new ApplyExpressionChecker(errors, repository, state, nv, false);
		Type fnt = context.mock(Type.class, "fn/2");
		Type nbr = context.mock(Type.class, "nbr");
		context.checking(new Expectations() {{
			allowing(fnt).argCount(); will(returnValue(2));
			oneOf(fnt).get(0); will(returnValue(nbr));
			oneOf(nbr).incorporates(pos, nbr); will(returnValue(true));
			oneOf(fnt).get(1); will(returnValue(nbr));
			oneOf(nbr).incorporates(pos, nbr); will(returnValue(true));
			oneOf(fnt).get(2); will(returnValue(nbr));
			oneOf(nv).result(nbr);
		}});
		UnresolvedOperator op = new UnresolvedOperator(pos, "+");
		FunctionDefinition fn = new FunctionDefinition(FunctionName.function(pos, null, "+"), 2, false);
		fn.bindType(fnt);
		op.bind(fn);
		NumericLiteral e1 = new NumericLiteral(pos, "42", 2);
		NumericLiteral e2 = new NumericLiteral(pos, "42", 2);
		ApplyExpr ae = new ApplyExpr(pos, op, e1, e2);
		aec.result(new ExprResult(pos, fnt));
		aec.result(new ExprResult(pos, nbr));
		aec.result(new ExprResult(pos, nbr));
		aec.leaveApplyExpr(ae);
	}

	@Test
	public void applyExpressionWithPolyApplyInstantiatesFreshUTs() {
		ApplyExpressionChecker aec = new ApplyExpressionChecker(errors, repository, state, nv, false);
		PolyType pt = new PolyType(pos, new SolidName(null, "A"));
		Type fnt = new Apply(pt, pt);
		Type nbr = LoadBuiltins.number;
		TypeConstraintSet ut = new TypeConstraintSet(repository, state, pos, "ut_A", "unknown");
		context.checking(new Expectations() {{
			oneOf(state).createUT(null, "instantiating A"); will(returnValue(ut));
			oneOf(nv).result(ut);
		}});
		UnresolvedVar f = new UnresolvedVar(pos, "f"); // A->A
		FunctionDefinition fn = new FunctionDefinition(FunctionName.function(pos, null, "f"), 2, false);
		fn.bindType(fnt);
		f.bind(fn);
		NumericLiteral e1 = new NumericLiteral(pos, "42", 2);
		ApplyExpr ae = new ApplyExpr(pos, f, e1);
		aec.result(new ExprResult(pos, fnt));
		aec.result(new ExprResult(pos, nbr));
		aec.leaveApplyExpr(ae);
		assertEquals(LoadBuiltins.number, ut.resolve(errors, true));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void leaveApplyExpressionWithInsufficientButValidTypesReturnsAnApply() {
		ApplyExpressionChecker aec = new ApplyExpressionChecker(errors, repository, state, nv, false);
		Type fnt = context.mock(Type.class, "fn/2");
		Type nbr = context.mock(Type.class, "nbr");
		Type string = context.mock(Type.class, "string");
		Type other = context.mock(Type.class, "other");
		context.checking(new Expectations() {{
			allowing(fnt).argCount(); will(returnValue(2));
			oneOf(fnt).get(0); will(returnValue(string));
			oneOf(string).incorporates(pos, string); will(returnValue(true));
			oneOf(fnt).get(1); will(returnValue(other));
			oneOf(fnt).get(2); will(returnValue(nbr));
			oneOf(nv).result(with(ApplyMatcher.type(Matchers.is(other), Matchers.is(nbr))));
		}});
		UnresolvedVar op = new UnresolvedVar(pos, "f");
		FunctionDefinition fn = new FunctionDefinition(FunctionName.function(pos, null, "f"), 2, false);
		fn.bindType(fnt);
		op.bind(fn);
		NumericLiteral e1 = new NumericLiteral(pos, "42", 2);
		ApplyExpr ae = new ApplyExpr(pos, op, e1);
		aec.result(new ExprResult(pos, fnt));
		aec.result(new ExprResult(pos, string));
		aec.leaveApplyExpr(ae);
	}

	@Test
	public void leaveApplyExpressionWithInvalidTypesThrowsAnError() {
		ApplyExpressionChecker aec = new ApplyExpressionChecker(errors, repository, state, nv, false);
		Type fnt = context.mock(Type.class, "fn/2");
		Type str = context.mock(Type.class, "str");
		Type nbr = context.mock(Type.class, "nbr");
		context.checking(new Expectations() {{
			oneOf(fnt).argCount(); will(returnValue(2));
			oneOf(fnt).get(0); will(returnValue(nbr));
			oneOf(fnt).get(1); will(returnValue(nbr));
			oneOf(nbr).incorporates(pos, nbr); will(returnValue(true));
			oneOf(nbr).incorporates(pos, str); will(returnValue(false));
			oneOf(nbr).signature(); will(returnValue("nbr"));
			oneOf(str).signature(); will(returnValue("str"));
			oneOf(errors).message(pos, "function '+' was expecting nbr not str");
			oneOf(nv).result(with(any(ErrorType.class)));
		}});
		UnresolvedOperator op = new UnresolvedOperator(pos, "+");
		FunctionDefinition fn = new FunctionDefinition(FunctionName.function(pos, null, "+"), 2, false);
		fn.bindType(nbr);
		op.bind(fn);
		NumericLiteral e1 = new NumericLiteral(pos, "42", 2);
		StringLiteral e2 = new StringLiteral(pos, "hello");
		ApplyExpr ae = new ApplyExpr(pos, op, e1, e2);
		aec.result(new ExprResult(pos, fnt));
		aec.result(new ExprResult(pos, nbr));
		aec.result(new ExprResult(pos, str));
		aec.leaveApplyExpr(ae);
	}

	@Test
	public void leaveApplyExpressionWithEarlierErrorTypesReturnsAnErrorTypeButDoesNotCascade() {
		ApplyExpressionChecker aec = new ApplyExpressionChecker(errors, repository, state, nv, false);
		Type fnt = context.mock(Type.class, "fn/2");
		Type err = new ErrorType();
		Type nbr = context.mock(Type.class, "nbr");
		context.checking(new Expectations() {{
			oneOf(fnt).argCount(); will(returnValue(2));
			oneOf(nv).result(with(any(ErrorType.class)));
		}});
		UnresolvedOperator op = new UnresolvedOperator(pos, "+");
		FunctionDefinition fn = new FunctionDefinition(FunctionName.function(pos, null, "+"), 2, false);
		fn.bindType(nbr);
		op.bind(fn);
		StringLiteral e2 = new StringLiteral(pos, "hello");
		NumericLiteral e1 = new NumericLiteral(pos, "42", 2);
		ApplyExpr ae = new ApplyExpr(pos, op, e1, e2);
		aec.result(new ExprResult(pos, fnt));
		aec.result(new ExprResult(pos, err));
		aec.result(new ExprResult(pos, nbr));
		aec.leaveApplyExpr(ae);
	}

	@Test
	public void leaveApplyExpressionAttachesConstraintsToPolyVarHolders() {
		ApplyExpressionChecker aec = new ApplyExpressionChecker(errors, repository, state, nv, false);
		Type fnt = context.mock(Type.class, "fn/2");
		Type nbr = LoadBuiltins.number;
		UnifiableType ut = context.mock(UnifiableType.class);
		FunctionName func = FunctionName.function(pos, null, "f");
		VarPattern funcVar = new VarPattern(pos, new VarName(pos, func, "x"));
		context.checking(new Expectations() {{
			allowing(fnt).argCount(); will(returnValue(2));
			oneOf(fnt).get(0); will(returnValue(nbr));
			oneOf(ut).incorporatedBy(pos, nbr);
			oneOf(fnt).get(1); will(returnValue(nbr));
			oneOf(fnt).get(2); will(returnValue(nbr));
			oneOf(nv).result(nbr);
		}});
		UnresolvedOperator op = new UnresolvedOperator(pos, "+");
		FunctionDefinition fn = new FunctionDefinition(FunctionName.function(pos, null, "+"), 2, false);
		fn.bindType(nbr);
		op.bind(fn);
		UnresolvedVar uv = new UnresolvedVar(pos, "x");
		uv.bind(funcVar);
		NumericLiteral e2 = new NumericLiteral(pos, "42", 2);
		ApplyExpr ae = new ApplyExpr(pos, op, uv, e2);
		aec.result(new ExprResult(pos, fnt));
		aec.result(new ExprResult(pos, ut));
		aec.result(new ExprResult(pos, nbr));
		aec.leaveApplyExpr(ae);
	}

	@Test
	public void leaveApplyExpressionCanHandleUnifiableTypesAsFunctionsProducingApplications() {
		ApplyExpressionChecker aec = new ApplyExpressionChecker(errors, repository, state, nv, false);
		Type nbr = context.mock(Type.class, "nbr");
		UnifiableType ut = new TypeConstraintSet(repository, state, pos, "tcs", "unknown");
		FunctionName func = FunctionName.function(pos, null, "f");
		VarPattern funcVar = new VarPattern(pos, new VarName(pos, func, "x"));
		UnifiableType result = context.mock(UnifiableType.class, "result");
		context.checking(new Expectations() {{
			oneOf(nv).result(with(any(UnifiableType.class)));
			oneOf(state).createUT(with(pos), with(any(String.class))); will(returnValue(result));
		}});
		UnresolvedVar fn = new UnresolvedVar(pos, "f");
		fn.bind(funcVar);
		NumericLiteral nl = new NumericLiteral(pos, "42", 2);
		ApplyExpr ae = new ApplyExpr(pos, fn, nl);
		aec.result(new ExprResult(pos, ut));
		aec.result(new ExprResult(pos, nbr));
		aec.leaveApplyExpr(ae);
	}

	@Test
	public void aUnifiableTypeCanBeAppliedToAUnifiableTypeWhichCreatesABond() {
		ApplyExpressionChecker aec = new ApplyExpressionChecker(errors, repository, state, nv, false);
		UnifiableType utF = new TypeConstraintSet(repository, state, pos, "func", "unknown");
		UnifiableType utV = context.mock(UnifiableType.class);
		FunctionName fname = FunctionName.function(pos, null, "f");
		VarPattern func = new VarPattern(pos, new VarName(pos, fname, "f"));
		VarPattern funcVar = new VarPattern(pos, new VarName(pos, fname, "x"));
		UnifiableType result = context.mock(UnifiableType.class, "result");
		context.checking(new Expectations() {{
			oneOf(state).createUT(with(pos), with(any(String.class))); will(returnValue(result));
			oneOf(utV).isUsed(pos);
			oneOf(nv).result(with(any(UnifiableType.class)));
		}});
		UnresolvedVar fn = new UnresolvedVar(pos, "f");
		fn.bind(func);
		UnresolvedVar var = new UnresolvedVar(pos, "x");
		var.bind(funcVar);
		ApplyExpr ae = new ApplyExpr(pos, fn, var);
		aec.result(new ExprResult(pos, utF));
		aec.result(new ExprResult(pos, utV));
		aec.leaveApplyExpr(ae);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void leaveApplyExpressionCanDealWithExplicitCurrying() {
		ApplyExpressionChecker aec = new ApplyExpressionChecker(errors, repository, state, nv, false);
		Type fnt = context.mock(Type.class, "fn/2");
		Type nbr = context.mock(Type.class, "nbr");
		context.checking(new Expectations() {{
			allowing(fnt).argCount(); will(returnValue(2));
			oneOf(fnt).get(0); will(returnValue(nbr));
			oneOf(fnt).get(1); will(returnValue(nbr));
			oneOf(nbr).incorporates(pos, nbr); will(returnValue(true));
			oneOf(fnt).get(2); will(returnValue(nbr));
			oneOf(nv).result(with(ApplyMatcher.type(Matchers.is(nbr), Matchers.is(nbr))));
		}});
		UnresolvedOperator op = new UnresolvedOperator(pos, "+");
		FunctionDefinition fn = new FunctionDefinition(FunctionName.function(pos, null, "+"), 2, false);
		fn.bindType(fnt);
		op.bind(fn);
		NumericLiteral e1 = new NumericLiteral(pos, "42", 2);
		ApplyExpr ae = new ApplyExpr(pos, op, new AnonymousVar(pos), e1);
		aec.result(new ExprResult(pos, fnt));
		aec.result(new ExprResult(pos, new CurryArgumentType(pos)));
		aec.result(new ExprResult(pos, nbr));
		aec.leaveApplyExpr(ae);
	}

	@Test
	public void leaveMemberExpressionCanFindAFieldInAStructDefn() {
		MemberExpressionChecker mec = new MemberExpressionChecker(errors, repository, state, nv, false);
		context.checking(new Expectations() {{
			oneOf(nv).result(LoadBuiltins.polyA);
		}});
		UnresolvedVar from = new UnresolvedVar(pos, "l");
		UnresolvedVar fld = new UnresolvedVar(pos, "head");
		MemberExpr dot = new MemberExpr(pos, from, fld);
		mec.result(new ExprResult(pos, LoadBuiltins.cons));
		mec.leaveMemberExpr(dot);
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void leaveMemberExpressionCanFindAMethodInAValidContract() {
		ContractDecl cd = new ContractDecl(pos, pos, ContractType.CONTRACT, new SolidName(pkg, "AContract"));
		List<TypedPattern> args = new ArrayList<>();
		ContractMethodDecl cmd = new ContractMethodDecl(pos, pos, pos, false, FunctionName.contractMethod(pos, cd.name(), "m"), args, null);
		cd.addMethod(cmd);
		cmd.bindType();
		
		MemberExpressionChecker mec = new MemberExpressionChecker(errors, repository, state, nv, false);
		context.checking(new Expectations() {{
			oneOf(nv).result(with(ApplyMatcher.type(Matchers.is(LoadBuiltins.contract), Matchers.is(LoadBuiltins.send))));
		}});
		UnresolvedVar from = new UnresolvedVar(pos, "obj");
		UnresolvedVar fld = new UnresolvedVar(pos, "m");
		MemberExpr dot = new MemberExpr(pos, from, fld);
		mec.result(new ExprResult(pos, cd));
		mec.leaveMemberExpr(dot);
	}
	
	@Test
	public void leaveMemberExpressionCannotFindAMethodThatDoesNotExist() {
		ContractDecl cd = new ContractDecl(pos, pos, ContractType.CONTRACT, new SolidName(pkg, "AContract"));
		List<TypedPattern> args = new ArrayList<>();
		ContractMethodDecl cmd = new ContractMethodDecl(pos, pos, pos, false, FunctionName.contractMethod(pos, cd.name(), "m"), args, null);
		cd.addMethod(cmd);
		cmd.bindType();
		
		MemberExpressionChecker mec = new MemberExpressionChecker(errors, repository, state, nv, false);
		context.checking(new Expectations() {{
			oneOf(errors).message(pos, "there is no method 'q' in test.repo.AContract");
			oneOf(nv).result(with(any(ErrorType.class)));
		}});
		UnresolvedVar from = new UnresolvedVar(pos, "obj");
		UnresolvedVar fld = new UnresolvedVar(pos, "q");
		MemberExpr dot = new MemberExpr(pos, from, fld);
		mec.result(new ExprResult(pos, cd));
		mec.leaveMemberExpr(dot);
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void leaveMemberExpressionCanHandleAnMethodWithArgumentsAndReturnSomethingThatLooksLikeAFunction() {
		ContractDecl cd = new ContractDecl(pos, pos, ContractType.CONTRACT, new SolidName(pkg, "AContract"));
		List<TypedPattern> args = new ArrayList<>();
		FunctionName mname = FunctionName.contractMethod(pos, cd.name(), "m");
		TypedPattern argx = new TypedPattern(pos, LoadBuiltins.stringTR, new VarName(pos, mname, "x"));
		argx.type.bind(LoadBuiltins.string);
		args.add(argx);
		ContractMethodDecl cmd = new ContractMethodDecl(pos, pos, pos, false, FunctionName.contractMethod(pos, cd.name(), "m"), args, null);
		cd.addMethod(cmd);
		cmd.bindType();
		
		MemberExpressionChecker mec = new MemberExpressionChecker(errors, repository, state, nv, false);
		context.checking(new Expectations() {{
			oneOf(nv).result(with(ApplyMatcher.type(Matchers.is(LoadBuiltins.string), Matchers.is(LoadBuiltins.contract), Matchers.is(LoadBuiltins.send))));
		}});
		UnresolvedVar from = new UnresolvedVar(pos, "obj");
		UnresolvedVar fld = new UnresolvedVar(pos, "m");
		MemberExpr dot = new MemberExpr(pos, from, fld);
		mec.result(new ExprResult(pos, cd));
		mec.leaveMemberExpr(dot);
	}

	@Test
	public void leaveMemberExpressionCanFindAnAccessorInAnObjectDefn() {
		SolidName on = new SolidName(pkg, "ObjDefn");
		ObjectDefn od = new ObjectDefn(pos, pos, on, true, new ArrayList<>());
		FunctionName an = FunctionName.function(pos, on, "acor");
		FunctionDefinition fn = new FunctionDefinition(an, 0, false);
		fn.bindType(LoadBuiltins.string);
		ObjectAccessor acor = new ObjectAccessor(od, fn);
		od.addAccessor(acor);
		TypeReference tr = new TypeReference(pos, "ObjDefn");
		tr.bind(od);
		
		MemberExpressionChecker mec = new MemberExpressionChecker(errors, repository, state, nv, false);
		context.checking(new Expectations() {{
			oneOf(nv).result(LoadBuiltins.string);
		}});
		UnresolvedVar from = new UnresolvedVar(pos, "obj");
		UnresolvedVar fld = new UnresolvedVar(pos, "acor");
		MemberExpr dot = new MemberExpr(pos, from, fld);
		mec.result(new ExprResult(pos, od));
		mec.leaveMemberExpr(dot);
	}
	

	// I think there are a number of other cases.  I'm not sure how many of them I actually want to allow and how many I view as errors
	// NOTE: the MemberExpr triples for "object vars", "object invocation" and "contract method invocation"; we definitely want to handle the object cases
	// objects:
	//   the object could be an "object" of some description and we want to extract a var
	//   we might want to invoke another method on an object (which would not be a contract decl)
	//   note the object could be "this"
	// unbound/AE cases:
	//   the object could be an unbound var and we need to (somehow) figure that out - the logic here would be to tell the UT that we wanted to dispatch a particular call & then return a new UT that would have to take the appropriate args in a surrounding AE if any
	//   the object could be the result of an AE itself that (for whatever reason) does not resolve to a ContractDecl immediately
}
