package test.tc3;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.ApplyExpr;
import org.flasck.flas.commonBase.NumericLiteral;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.UnresolvedOperator;
import org.flasck.flas.repository.LoadBuiltins;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.tc3.ApplyExpressionChecker;
import org.flasck.flas.tc3.CurrentTCState;
import org.flasck.flas.tc3.ExpressionChecker.ExprResult;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Rule;
import org.junit.Test;

import flas.matchers.ConsolidatedTypeMatcher;
import flas.matchers.PolyTypeMatcher;

public class ListBuilding {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	private InputPosition pos = new InputPosition("-", 1, 0, "hello");
	private ErrorReporter errors = context.mock(ErrorReporter.class);
	private NestedVisitor nv = context.mock(NestedVisitor.class);
	private CurrentTCState state = context.mock(CurrentTCState.class);

	@Test
	public void nilReturnsNil() {
		ApplyExpressionChecker aec = new ApplyExpressionChecker(errors, state, nv);
		context.checking(new Expectations() {{
			oneOf(nv).result(LoadBuiltins.nil);
		}});
		UnresolvedOperator op = new UnresolvedOperator(pos, "[]");
		FunctionDefinition fn = new FunctionDefinition(FunctionName.function(pos, null, "[]"), 0);
		op.bind(fn);
		ApplyExpr ae = new ApplyExpr(pos, op);
		aec.result(new ExprResult(LoadBuiltins.nil)); // the "operator" for any list is always Nil
		aec.leaveApplyExpr(ae);
	}

	@SuppressWarnings({ "unchecked" })
	@Test
	public void consOfASingleArgumentReturnsAPolyInstanceWithThatType() {
		ApplyExpressionChecker aec = new ApplyExpressionChecker(errors, state, nv);
		context.checking(new Expectations() {{
			oneOf(nv).result(with(PolyTypeMatcher.of(LoadBuiltins.cons, Matchers.is(LoadBuiltins.number))));
		}});
		UnresolvedOperator op = new UnresolvedOperator(pos, "[]");
		FunctionDefinition fn = new FunctionDefinition(FunctionName.function(pos, null, "[]"), 0);
		op.bind(fn);
		NumericLiteral e1 = new NumericLiteral(pos, "42", 2);
		ApplyExpr ae = new ApplyExpr(pos, op, e1);
		aec.result(new ExprResult(LoadBuiltins.nil)); // the "operator" for any list is always Nil
		aec.result(new ExprResult(LoadBuiltins.number));
		aec.leaveApplyExpr(ae);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void consOfATwoStringsReturnsAPolyInstanceOfString() {
		ApplyExpressionChecker aec = new ApplyExpressionChecker(errors, state, nv);
		context.checking(new Expectations() {{
			oneOf(nv).result(with(PolyTypeMatcher.of(LoadBuiltins.cons, (Matcher)ConsolidatedTypeMatcher.with(Matchers.is(LoadBuiltins.string), Matchers.is(LoadBuiltins.string)))));
		}});
		UnresolvedOperator op = new UnresolvedOperator(pos, "[]");
		FunctionDefinition fn = new FunctionDefinition(FunctionName.function(pos, null, "[]"), 0);
		op.bind(fn);
		NumericLiteral e1 = new NumericLiteral(pos, "42", 2);
		ApplyExpr ae = new ApplyExpr(pos, op, e1);
		aec.result(new ExprResult(LoadBuiltins.nil)); // the "operator" for any list is always Nil
		aec.result(new ExprResult(LoadBuiltins.string));
		aec.result(new ExprResult(LoadBuiltins.string));
		aec.leaveApplyExpr(ae);
	}
}
