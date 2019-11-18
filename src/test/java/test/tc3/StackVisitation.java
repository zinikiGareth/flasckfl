package test.tc3;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.ApplyExpr;
import org.flasck.flas.commonBase.MemberExpr;
import org.flasck.flas.commonBase.NumericLiteral;
import org.flasck.flas.commonBase.Pattern;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.commonBase.names.SolidName;
import org.flasck.flas.commonBase.names.VarName;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.ContractDecl;
import org.flasck.flas.parsedForm.ContractMethodDecl;
import org.flasck.flas.parsedForm.ContractMethodDir;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.parsedForm.ObjectMethod;
import org.flasck.flas.parsedForm.PolyType;
import org.flasck.flas.parsedForm.SendMessage;
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
import org.flasck.flas.tc3.Type;
import org.flasck.flas.tc3.TypeConstraintSet;
import org.flasck.flas.tc3.UnifiableType;
import org.hamcrest.Matchers;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Rule;
import org.junit.Test;

import flas.matchers.ApplyMatcher;

public class StackVisitation {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	private InputPosition pos = new InputPosition("-", 1, 0, "hello");
	private final PackageName pkg = new PackageName("test.repo");
	private ErrorReporter errors = context.mock(ErrorReporter.class);
	private RepositoryReader repository = context.mock(RepositoryReader.class);
	private NestedVisitor nv = context.mock(NestedVisitor.class);
	private CurrentTCState state = context.mock(CurrentTCState.class);

	@Test
	public void whenWeVisitAFunctionWePushAFunctionChecker() {
		FunctionName name = FunctionName.function(pos, null, "f");
		FunctionIntro fi = new FunctionIntro(name, new ArrayList<>());
		fi.bindTree(new HSIArgsTree(0));
		context.checking(new Expectations() {{
			oneOf(nv).push(with(any(FunctionChecker.class)));
		}});
		GroupChecker gc = new GroupChecker(errors, repository, nv, null);
		gc.visitFunction(new FunctionDefinition(name, 0));
	}

	@Test
	public void theResultIsWhatWeEndUpSpittingOut() {
		FunctionName name = FunctionName.function(pos, null, "f");
		FunctionDefinition fn = new FunctionDefinition(name, 0);
		FunctionIntro fi = new FunctionIntro(name, new ArrayList<>());
		fi.bindTree(new HSIArgsTree(0));
		fn.intro(fi);
		Type ty = context.mock(Type.class, "ty");
		Type xx = context.mock(Type.class, "xx");
		UnifiableType utf = context.mock(UnifiableType.class, "utf");
		context.checking(new Expectations() {{
			oneOf(nv).push(with(any(FunctionChecker.class)));
			allowing(state).requireVarConstraints(pos, "f"); will(returnValue(utf));
			oneOf(utf).determinedType(ty);
			oneOf(state).resolveAll(false);
			oneOf(utf).resolve(false); will(returnValue(xx));
			oneOf(utf).rebind(xx);
			oneOf(state).enhanceAllMutualUTs();
			oneOf(state).resolveAll(true);
			oneOf(state).bindVarPatternTypes();
			oneOf(nv).result(null);
		}});
		GroupChecker gc = new GroupChecker(errors, repository, nv, state);
		gc.visitFunction(fn);
		gc.result(ty);
		gc.leaveFunctionIntro(fi);
		gc.leaveFunction(fn);
		gc.leaveFunctionGroup(null);
		assertEquals(ty, fn.type());
	}

	@Test
	public void weCanTypecheckMethods() {
		FunctionName name = FunctionName.standaloneMethod(pos, null, "meth");
		ObjectMethod om = new ObjectMethod(pos, name, new ArrayList<>());
		NumericLiteral e1 = new NumericLiteral(pos, "42", 2);
		om.sendMessage(new SendMessage(pos, e1));

		FunctionIntro fi = new FunctionIntro(name, new ArrayList<>());
		fi.bindTree(new HSIArgsTree(0));
		om.conversion(Arrays.asList(fi));
		
		Type ty = context.mock(Type.class);
		context.checking(new Expectations() {{
			oneOf(nv).push(with(any(FunctionChecker.class)));
		}});
		GroupChecker gc = new GroupChecker(errors, repository, nv, state);
		gc.visitObjectMethod(om);
		context.assertIsSatisfied();
		UnifiableType utm = context.mock(UnifiableType.class, "utm");
		Type xx = context.mock(Type.class, "xx");
		context.checking(new Expectations() {{
			allowing(state).requireVarConstraints(pos, "meth"); will(returnValue(utm));
			oneOf(utm).determinedType(ty);
			oneOf(state).resolveAll(false);
			oneOf(utm).resolve(false); will(returnValue(xx));
			oneOf(utm).rebind(xx);
			oneOf(state).enhanceAllMutualUTs();
			oneOf(state).resolveAll(true);
			oneOf(state).bindVarPatternTypes();
			oneOf(nv).result(null);
		}});
		gc.result(ty);
		gc.leaveObjectMethod(om);
		gc.leaveFunctionGroup(null);
		assertEquals(ty, om.type());
	}

	@Test
	public void applyExpressionsPushAnotherMatcher() {
		ExpressionChecker ec = new ExpressionChecker(errors, state, nv);
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
		ExpressionChecker ec = new ExpressionChecker(errors, state, nv);
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
		ApplyExpressionChecker aec = new ApplyExpressionChecker(errors, state, nv);
		context.checking(new Expectations() {{
			oneOf(nv).push(with(any(ExpressionChecker.class)));
		}});
		UnresolvedOperator op = new UnresolvedOperator(pos, "+");
		aec.visitExpr(op, 2);
	}

	@Test
	public void applyCheckerPushesOnMemberExpr() {
		ApplyExpressionChecker aec = new ApplyExpressionChecker(errors, state, nv);
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
		ApplyExpressionChecker aec = new ApplyExpressionChecker(errors, state, nv);
		Type fnt = context.mock(Type.class, "fn/2");
		Type nbr = context.mock(Type.class, "nbr");
		context.checking(new Expectations() {{
			allowing(fnt).argCount(); will(returnValue(2));
			oneOf(fnt).get(0); will(returnValue(nbr));
			oneOf(nbr).incorporates(nbr); will(returnValue(true));
			oneOf(fnt).get(1); will(returnValue(nbr));
			oneOf(nbr).incorporates(nbr); will(returnValue(true));
			oneOf(fnt).get(2); will(returnValue(nbr));
			oneOf(nv).result(nbr);
		}});
		UnresolvedOperator op = new UnresolvedOperator(pos, "+");
		FunctionDefinition fn = new FunctionDefinition(FunctionName.function(pos, null, "+"), 2);
		fn.bindType(fnt);
		op.bind(fn);
		NumericLiteral e1 = new NumericLiteral(pos, "42", 2);
		NumericLiteral e2 = new NumericLiteral(pos, "42", 2);
		ApplyExpr ae = new ApplyExpr(pos, op, e1, e2);
		aec.result(new ExprResult(fnt));
		aec.result(new ExprResult(nbr));
		aec.result(new ExprResult(nbr));
		aec.leaveApplyExpr(ae);
	}

	@Test
	public void applyExpressionWithPolyApplyInstantiatesFreshUTs() {
		ApplyExpressionChecker aec = new ApplyExpressionChecker(errors, state, nv);
		PolyType pt = new PolyType(pos, "A");
		Type fnt = new Apply(pt, pt);
		Type nbr = LoadBuiltins.number;
		TypeConstraintSet ut = new TypeConstraintSet(repository, state, pos, "ut_A");
		context.checking(new Expectations() {{
			oneOf(state).createUT(); will(returnValue(ut));
			oneOf(nv).result(ut);
		}});
		UnresolvedVar f = new UnresolvedVar(pos, "f"); // A->A
		FunctionDefinition fn = new FunctionDefinition(FunctionName.function(pos, null, "f"), 2);
		fn.bindType(fnt);
		f.bind(fn);
		NumericLiteral e1 = new NumericLiteral(pos, "42", 2);
		ApplyExpr ae = new ApplyExpr(pos, f, e1);
		aec.result(new ExprResult(fnt));
		aec.result(new ExprResult(nbr));
		aec.leaveApplyExpr(ae);
		assertEquals(LoadBuiltins.number, ut.resolve());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void leaveApplyExpressionWithInsufficientButValidTypesReturnsAnApply() {
		ApplyExpressionChecker aec = new ApplyExpressionChecker(errors, state, nv);
		Type fnt = context.mock(Type.class, "fn/2");
		Type nbr = context.mock(Type.class, "nbr");
		Type string = context.mock(Type.class, "string");
		Type other = context.mock(Type.class, "other");
		context.checking(new Expectations() {{
			allowing(fnt).argCount(); will(returnValue(2));
			oneOf(fnt).get(0); will(returnValue(string));
			oneOf(string).incorporates(string); will(returnValue(true));
			oneOf(fnt).get(1); will(returnValue(other));
			oneOf(fnt).get(2); will(returnValue(nbr));
			oneOf(nv).result(with(ApplyMatcher.type(Matchers.is(other), Matchers.is(nbr))));
		}});
		UnresolvedVar op = new UnresolvedVar(pos, "f");
		FunctionDefinition fn = new FunctionDefinition(FunctionName.function(pos, null, "f"), 2);
		fn.bindType(fnt);
		op.bind(fn);
		NumericLiteral e1 = new NumericLiteral(pos, "42", 2);
		ApplyExpr ae = new ApplyExpr(pos, op, e1);
		aec.result(new ExprResult(fnt));
		aec.result(new ExprResult(string));
		aec.leaveApplyExpr(ae);
	}

	@Test
	public void leaveApplyExpressionWithInvalidTypesThrowsAnError() {
		ApplyExpressionChecker aec = new ApplyExpressionChecker(errors, state, nv);
		Type fnt = context.mock(Type.class, "fn/2");
		Type str = context.mock(Type.class, "str");
		Type nbr = context.mock(Type.class, "nbr");
		context.checking(new Expectations() {{
			oneOf(fnt).argCount(); will(returnValue(2));
			oneOf(fnt).get(0); will(returnValue(nbr));
			oneOf(fnt).get(1); will(returnValue(nbr));
			oneOf(nbr).incorporates(nbr); will(returnValue(true));
			oneOf(nbr).incorporates(str); will(returnValue(false));
			oneOf(nbr).signature(); will(returnValue("nbr"));
			oneOf(str).signature(); will(returnValue("str"));
			oneOf(errors).message(pos, "function '+' was expecting nbr not str");
			oneOf(nv).result(with(any(ErrorType.class)));
		}});
		UnresolvedOperator op = new UnresolvedOperator(pos, "+");
		FunctionDefinition fn = new FunctionDefinition(FunctionName.function(pos, null, "+"), 2);
		fn.bindType(nbr);
		op.bind(fn);
		NumericLiteral e1 = new NumericLiteral(pos, "42", 2);
		StringLiteral e2 = new StringLiteral(pos, "hello");
		ApplyExpr ae = new ApplyExpr(pos, op, e1, e2);
		aec.result(new ExprResult(fnt));
		aec.result(new ExprResult(nbr));
		aec.result(new ExprResult(str));
		aec.leaveApplyExpr(ae);
	}

	@Test
	public void leaveApplyExpressionWithEarlierErrorTypesReturnsAnErrorTypeButDoesNotCascade() {
		ApplyExpressionChecker aec = new ApplyExpressionChecker(errors, state, nv);
		Type fnt = context.mock(Type.class, "fn/2");
		Type err = new ErrorType();
		Type nbr = context.mock(Type.class, "nbr");
		context.checking(new Expectations() {{
			oneOf(fnt).argCount(); will(returnValue(2));
			oneOf(nv).result(with(any(ErrorType.class)));
		}});
		UnresolvedOperator op = new UnresolvedOperator(pos, "+");
		FunctionDefinition fn = new FunctionDefinition(FunctionName.function(pos, null, "+"), 2);
		fn.bindType(nbr);
		op.bind(fn);
		StringLiteral e2 = new StringLiteral(pos, "hello");
		NumericLiteral e1 = new NumericLiteral(pos, "42", 2);
		ApplyExpr ae = new ApplyExpr(pos, op, e1, e2);
		aec.result(new ExprResult(fnt));
		aec.result(new ExprResult(err));
		aec.result(new ExprResult(nbr));
		aec.leaveApplyExpr(ae);
	}

	@Test
	public void leaveApplyExpressionAttachesConstraintsToPolyVarHolders() {
		ApplyExpressionChecker aec = new ApplyExpressionChecker(errors, state, nv);
		Type fnt = context.mock(Type.class, "fn/2");
		Type nbr = context.mock(Type.class, "nbr");
		UnifiableType ut = context.mock(UnifiableType.class);
		FunctionName func = FunctionName.function(pos, null, "f");
		VarPattern funcVar = new VarPattern(pos, new VarName(pos, func, "x"));
		context.checking(new Expectations() {{
			allowing(fnt).argCount(); will(returnValue(2));
			oneOf(fnt).get(0); will(returnValue(nbr));
			oneOf(ut).incorporatedBy(pos, nbr);
			oneOf(fnt).get(1); will(returnValue(nbr));
			oneOf(nbr).incorporates(nbr); will(returnValue(true));
			oneOf(fnt).get(2); will(returnValue(nbr));
			oneOf(nv).result(nbr);
		}});
		UnresolvedOperator op = new UnresolvedOperator(pos, "+");
		FunctionDefinition fn = new FunctionDefinition(FunctionName.function(pos, null, "+"), 2);
		fn.bindType(nbr);
		op.bind(fn);
		UnresolvedVar uv = new UnresolvedVar(pos, "x");
		uv.bind(funcVar);
		NumericLiteral e2 = new NumericLiteral(pos, "42", 2);
		ApplyExpr ae = new ApplyExpr(pos, op, uv, e2);
		aec.result(new ExprResult(fnt));
		aec.result(new ExprResult(ut));
		aec.result(new ExprResult(nbr));
		aec.leaveApplyExpr(ae);
	}

	@Test
	public void leaveApplyExpressionCanHandleUnifiableTypesAsFunctionsProducingApplications() {
		ApplyExpressionChecker aec = new ApplyExpressionChecker(errors, state, nv);
		Type nbr = context.mock(Type.class, "nbr");
		UnifiableType ut = new TypeConstraintSet(repository, state, pos, "tcs");
		FunctionName func = FunctionName.function(pos, null, "f");
		VarPattern funcVar = new VarPattern(pos, new VarName(pos, func, "x"));
		UnifiableType result = context.mock(UnifiableType.class, "result");
		context.checking(new Expectations() {{
			oneOf(nv).result(with(any(UnifiableType.class)));
			oneOf(state).createUT(); will(returnValue(result));
		}});
		UnresolvedVar fn = new UnresolvedVar(pos, "f");
		fn.bind(funcVar);
		NumericLiteral nl = new NumericLiteral(pos, "42", 2);
		ApplyExpr ae = new ApplyExpr(pos, fn, nl);
		aec.result(new ExprResult(ut));
		aec.result(new ExprResult(nbr));
		aec.leaveApplyExpr(ae);
	}

	@Test
	public void aUnifiableTypeCanBeAppliedToAUnifiableTypeWhichCreatesABond() {
		ApplyExpressionChecker aec = new ApplyExpressionChecker(errors, state, nv);
		UnifiableType utF = new TypeConstraintSet(repository, state, pos, "func");
		UnifiableType utV = context.mock(UnifiableType.class);
		FunctionName fname = FunctionName.function(pos, null, "f");
		VarPattern func = new VarPattern(pos, new VarName(pos, fname, "f"));
		VarPattern funcVar = new VarPattern(pos, new VarName(pos, fname, "x"));
		UnifiableType result = context.mock(UnifiableType.class, "result");
		context.checking(new Expectations() {{
			oneOf(state).createUT(); will(returnValue(result));
			oneOf(utV).isUsed();
			oneOf(nv).result(with(any(UnifiableType.class)));
		}});
		UnresolvedVar fn = new UnresolvedVar(pos, "f");
		fn.bind(func);
		UnresolvedVar var = new UnresolvedVar(pos, "x");
		var.bind(funcVar);
		ApplyExpr ae = new ApplyExpr(pos, fn, var);
		aec.result(new ExprResult(utF));
		aec.result(new ExprResult(utV));
		aec.leaveApplyExpr(ae);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void leaveApplyExpressionCanDealWithExplicitCurrying() {
		ApplyExpressionChecker aec = new ApplyExpressionChecker(errors, state, nv);
		Type fnt = context.mock(Type.class, "fn/2");
		Type nbr = context.mock(Type.class, "nbr");
		context.checking(new Expectations() {{
			allowing(fnt).argCount(); will(returnValue(2));
			oneOf(fnt).get(0); will(returnValue(nbr));
			oneOf(fnt).get(1); will(returnValue(nbr));
			oneOf(nbr).incorporates(nbr); will(returnValue(true));
			oneOf(fnt).get(2); will(returnValue(nbr));
			oneOf(nv).result(with(ApplyMatcher.type(Matchers.is(nbr), Matchers.is(nbr))));
		}});
		UnresolvedOperator op = new UnresolvedOperator(pos, "+");
		FunctionDefinition fn = new FunctionDefinition(FunctionName.function(pos, null, "+"), 2);
		fn.bindType(fnt);
		op.bind(fn);
		NumericLiteral e1 = new NumericLiteral(pos, "42", 2);
		ApplyExpr ae = new ApplyExpr(pos, op, LoadBuiltins.ca, e1);
		aec.result(new ExprResult(fnt));
		aec.result(new ExprResult(new CurryArgumentType(pos)));
		aec.result(new ExprResult(nbr));
		aec.leaveApplyExpr(ae);
	}

	@Test
	public void leaveMemberExpressionCanFindAFieldInAStructDefn() {
		MemberExpressionChecker mec = new MemberExpressionChecker(errors, state, nv);
		context.checking(new Expectations() {{
			oneOf(nv).result(LoadBuiltins.polyA);
		}});
		UnresolvedVar from = new UnresolvedVar(pos, "l");
		UnresolvedVar fld = new UnresolvedVar(pos, "head");
		MemberExpr dot = new MemberExpr(pos, from, fld);
		mec.result(new ExprResult(LoadBuiltins.cons));
		mec.leaveMemberExpr(dot);
	}
	
	@Test
	public void leaveMemberExpressionCanFindAMethodInAValidContract() {
		ContractDecl cd = new ContractDecl(pos, pos, new SolidName(pkg, "AContract"));
		List<Pattern> args = new ArrayList<>();
		ContractMethodDecl cmd = new ContractMethodDecl(pos, pos, pos, false, ContractMethodDir.UP, FunctionName.contractMethod(pos, cd.name(), "m"), args);
		cd.addMethod(cmd);
		cmd.bindType();
		
		MemberExpressionChecker mec = new MemberExpressionChecker(errors, state, nv);
		context.checking(new Expectations() {{
			oneOf(nv).result(LoadBuiltins.send);
		}});
		UnresolvedVar from = new UnresolvedVar(pos, "obj");
		UnresolvedVar fld = new UnresolvedVar(pos, "m");
		MemberExpr dot = new MemberExpr(pos, from, fld);
		mec.result(new ExprResult(cd));
		mec.leaveMemberExpr(dot);
	}
	
	@Test
	public void leaveMemberExpressionCannotFindAMethodThatDoesNotExist() {
		ContractDecl cd = new ContractDecl(pos, pos, new SolidName(pkg, "AContract"));
		List<Pattern> args = new ArrayList<>();
		ContractMethodDecl cmd = new ContractMethodDecl(pos, pos, pos, false, ContractMethodDir.UP, FunctionName.contractMethod(pos, cd.name(), "m"), args);
		cd.addMethod(cmd);
		cmd.bindType();
		
		MemberExpressionChecker mec = new MemberExpressionChecker(errors, state, nv);
		context.checking(new Expectations() {{
			oneOf(errors).message(pos, "there is no method 'q' in test.repo.AContract");
			oneOf(nv).result(with(any(ErrorType.class)));
		}});
		UnresolvedVar from = new UnresolvedVar(pos, "obj");
		UnresolvedVar fld = new UnresolvedVar(pos, "q");
		MemberExpr dot = new MemberExpr(pos, from, fld);
		mec.result(new ExprResult(cd));
		mec.leaveMemberExpr(dot);
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void leaveMemberExpressionCanHandleAnMethodWithArgumentsAndReturnSomethingThatLooksLikeAFunction() {
		ContractDecl cd = new ContractDecl(pos, pos, new SolidName(pkg, "AContract"));
		List<Pattern> args = new ArrayList<>();
		FunctionName mname = FunctionName.contractMethod(pos, cd.name(), "m");
		TypedPattern argx = new TypedPattern(pos, LoadBuiltins.stringTR, new VarName(pos, mname, "x"));
		argx.type.bind(LoadBuiltins.string);
		args.add(argx);
		ContractMethodDecl cmd = new ContractMethodDecl(pos, pos, pos, false, ContractMethodDir.UP, FunctionName.contractMethod(pos, cd.name(), "m"), args);
		cd.addMethod(cmd);
		cmd.bindType();
		
		MemberExpressionChecker mec = new MemberExpressionChecker(errors, state, nv);
		context.checking(new Expectations() {{
			oneOf(nv).result(with(ApplyMatcher.type(Matchers.is(LoadBuiltins.string), Matchers.is(LoadBuiltins.send))));
		}});
		UnresolvedVar from = new UnresolvedVar(pos, "obj");
		UnresolvedVar fld = new UnresolvedVar(pos, "m");
		MemberExpr dot = new MemberExpr(pos, from, fld);
		mec.result(new ExprResult(cd));
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
