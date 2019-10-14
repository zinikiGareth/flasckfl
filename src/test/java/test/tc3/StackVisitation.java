package test.tc3;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.ApplyExpr;
import org.flasck.flas.commonBase.NumericLiteral;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.VarName;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.parsedForm.UnresolvedOperator;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.parsedForm.VarPattern;
import org.flasck.flas.patterns.HSIArgsTree;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.repository.RepositoryReader;
import org.flasck.flas.tc3.ApplyExpressionChecker;
import org.flasck.flas.tc3.CurrentTCState;
import org.flasck.flas.tc3.ErrorType;
import org.flasck.flas.tc3.ExpressionChecker;
import org.flasck.flas.tc3.GroupChecker;
import org.flasck.flas.tc3.Type;
import org.flasck.flas.tc3.UnifiableType;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Rule;
import org.junit.Test;

import test.patterns.PatternExtraction.REType;

public class StackVisitation {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	private InputPosition pos = new InputPosition("-", 1, 0, "hello");
	private ErrorReporter errors = context.mock(ErrorReporter.class);
	private RepositoryReader repository = context.mock(RepositoryReader.class);
	private NestedVisitor nv = context.mock(NestedVisitor.class);
	private CurrentTCState state = context.mock(CurrentTCState.class);

	@Test
	public void whenWeVisitAFunctionIntroWePushAnExpressionMatcher() {
		FunctionName name = FunctionName.function(pos, null, "f");
		FunctionIntro fi = new FunctionIntro(name, new ArrayList<>());
		fi.bindTree(new HSIArgsTree(0));
		context.checking(new Expectations() {{
			oneOf(nv).push(with(any(ExpressionChecker.class)));
		}});
		GroupChecker tc = new GroupChecker(errors, repository, nv, null);
		tc.visitFunctionIntro(fi);
	}

	@Test
	public void theResultIsWhatWeEndUpSpittingOut() {
		FunctionName name = FunctionName.function(pos, null, "f");
		FunctionDefinition fn = new FunctionDefinition(name, 0);
		FunctionIntro fi = new FunctionIntro(name, new ArrayList<>());
		fi.bindTree(new HSIArgsTree(0));
		fn.intro(fi);
		Type ty = context.mock(Type.class);
		context.checking(new Expectations() {{
			oneOf(nv).push(with(any(ExpressionChecker.class)));
		}});
		GroupChecker tc = new GroupChecker(errors, repository, nv, null);
		tc.visitFunctionIntro(fi);
		tc.result(ty);
		tc.leaveFunctionIntro(fi);
		tc.leaveFunction(fn);
		assertEquals(ty, fn.type());
	}

	@Test
	public void applyExpressionsPushAnotherMatcher() {
		ExpressionChecker ec = new ExpressionChecker(errors, repository, state, nv);
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
	public void applyExpressionCheckerAutoPushesOnExpr() {
		ApplyExpressionChecker aec = new ApplyExpressionChecker(errors, repository, state, nv);
		context.checking(new Expectations() {{
			oneOf(nv).push(with(any(ExpressionChecker.class)));
		}});
		UnresolvedOperator op = new UnresolvedOperator(pos, "+");
		aec.visitExpr(op, 2);
	}

	@Test
	public void leaveApplyExpressionWithValidTypesReturnsResult() {
		ApplyExpressionChecker aec = new ApplyExpressionChecker(errors, repository, state, nv);
		Type fnt = context.mock(Type.class, "fn/2");
		Type nbr = context.mock(Type.class, "nbr");
		context.checking(new Expectations() {{
			oneOf(fnt).argCount(); will(returnValue(2));
			oneOf(fnt).get(0); will(returnValue(nbr));
			oneOf(nbr).incorporates(nbr); will(returnValue(true));
			oneOf(fnt).get(1); will(returnValue(nbr));
			oneOf(nbr).incorporates(nbr); will(returnValue(true));
			oneOf(fnt).get(2); will(returnValue(nbr));
			oneOf(nv).result(nbr);
		}});
		UnresolvedOperator op = new UnresolvedOperator(pos, "+");
		FunctionDefinition fn = new FunctionDefinition(FunctionName.function(pos, null, "+"), 2);
		fn.bindType(nbr);
		op.bind(fn);
		NumericLiteral e1 = new NumericLiteral(pos, "42", 2);
		NumericLiteral e2 = new NumericLiteral(pos, "42", 2);
		ApplyExpr ae = new ApplyExpr(pos, op, e1, e2);
		aec.result(fnt);
		aec.result(nbr);
		aec.result(nbr);
		aec.leaveApplyExpr(ae);
	}

	@Test
	public void leaveApplyExpressionWithInvalidTypesThrowsAnError() {
		ApplyExpressionChecker aec = new ApplyExpressionChecker(errors, repository, state, nv);
		Type fnt = context.mock(Type.class, "fn/2");
		Type str = context.mock(Type.class, "str");
		Type nbr = context.mock(Type.class, "nbr");
		context.checking(new Expectations() {{
			oneOf(fnt).argCount(); will(returnValue(2));
			oneOf(fnt).get(0); will(returnValue(nbr));
			oneOf(fnt).get(1); will(returnValue(nbr));
			oneOf(nbr).incorporates(nbr); will(returnValue(true));
			oneOf(nbr).incorporates(str); will(returnValue(false));
			oneOf(errors).message(pos, "typing: nbr str");
			oneOf(nv).result(with(any(ErrorType.class)));
		}});
		UnresolvedOperator op = new UnresolvedOperator(pos, "+");
		FunctionDefinition fn = new FunctionDefinition(FunctionName.function(pos, null, "+"), 2);
		fn.bindType(nbr);
		op.bind(fn);
		NumericLiteral e1 = new NumericLiteral(pos, "42", 2);
		StringLiteral e2 = new StringLiteral(pos, "hello");
		ApplyExpr ae = new ApplyExpr(pos, op, e1, e2);
		aec.result(fnt);
		aec.result(nbr);
		aec.result(str);
		aec.leaveApplyExpr(ae);
	}

	@Test
	public void leaveApplyExpressionWithEarlierErrorTypesReturnsAnErrorTypeButDoesNotCascade() {
		ApplyExpressionChecker aec = new ApplyExpressionChecker(errors, repository, state, nv);
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
		aec.result(fnt);
		aec.result(err);
		aec.result(nbr);
		aec.leaveApplyExpr(ae);
	}

	@Test
	public void leaveApplyExpressionAttachesConstraintsToPolyVarHolders() {
		ApplyExpressionChecker aec = new ApplyExpressionChecker(errors, repository, state, nv);
		Type fnt = context.mock(Type.class, "fn/2");
		Type nbr = context.mock(Type.class, "nbr");
		UnifiableType ut = context.mock(UnifiableType.class);
		FunctionName func = FunctionName.function(pos, null, "f");
		VarPattern funcVar = new VarPattern(pos, new VarName(pos, func, "x"));
		context.checking(new Expectations() {{
			oneOf(fnt).argCount(); will(returnValue(2));
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
		aec.result(fnt);
		aec.result(ut);
		aec.result(nbr);
		aec.leaveApplyExpr(ae);
	}

	@Test
	public void leaveApplyExpressionHandlesListsAsASpecialCase() {
		ApplyExpressionChecker aec = new ApplyExpressionChecker(errors, repository, state, nv);
		REType cons = context.mock(REType.class, "cons");
		Type fnt = context.mock(Type.class, "fn/2");
		Type nbr = context.mock(Type.class, "nbr");
		context.checking(new Expectations() {{
			oneOf(repository).get("Cons"); will(returnValue(cons));
			oneOf(nv).result(cons);
		}});
		UnresolvedOperator op = new UnresolvedOperator(pos, "[]");
		FunctionDefinition fn = new FunctionDefinition(FunctionName.function(pos, null, "[]"), 0);
		op.bind(fn);
		NumericLiteral e1 = new NumericLiteral(pos, "42", 2);
		ApplyExpr ae = new ApplyExpr(pos, op, e1);
		aec.result(fnt);
		aec.result(nbr);
		aec.leaveApplyExpr(ae);
	}
}
