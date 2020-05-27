package test.tc3;

import java.util.Arrays;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.ApplyExpr;
import org.flasck.flas.commonBase.NumericLiteral;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.UnresolvedOperator;
import org.flasck.flas.repository.LoadBuiltins;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.repository.RepositoryReader;
import org.flasck.flas.tc3.ApplyExpressionChecker;
import org.flasck.flas.tc3.CurrentTCState;
import org.flasck.flas.tc3.PosType;
import org.flasck.flas.tc3.ExpressionChecker.ExprResult;
import org.hamcrest.Matchers;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Rule;
import org.junit.Test;

import flas.matchers.PolyInstanceMatcher;

public class ListBuilding {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	private InputPosition pos = new InputPosition("-", 1, 0, "hello");
	private ErrorReporter errors = context.mock(ErrorReporter.class);
	private NestedVisitor nv = context.mock(NestedVisitor.class);
	private CurrentTCState state = context.mock(CurrentTCState.class);
	private RepositoryReader repository = context.mock(RepositoryReader.class);

	@Test
	public void nilReturnsNil() {
		ApplyExpressionChecker aec = new ApplyExpressionChecker(errors, repository, state, nv, false);
		context.checking(new Expectations() {{
			oneOf(nv).result(LoadBuiltins.nil);
		}});
		UnresolvedOperator op = new UnresolvedOperator(pos, "[]");
		FunctionDefinition fn = new FunctionDefinition(FunctionName.function(pos, null, "[]"), 0, null);
		op.bind(fn);
		ApplyExpr ae = new ApplyExpr(pos, op);
		aec.result(new ExprResult(pos, LoadBuiltins.nil)); // the "operator" for any list is always Nil
		aec.leaveApplyExpr(ae);
	}

	@SuppressWarnings({ "unchecked" })
	@Test
	public void consOfASingleArgumentReturnsAPolyInstanceWithThatType() {
		ApplyExpressionChecker aec = new ApplyExpressionChecker(errors, repository, state, nv, false);
		context.checking(new Expectations() {{
			oneOf(state).consolidate(pos, Arrays.asList(new PosType(pos, LoadBuiltins.number))); will(returnValue(new PosType(pos, LoadBuiltins.number)));
			oneOf(nv).result(with(PolyInstanceMatcher.of(LoadBuiltins.cons, Matchers.is(LoadBuiltins.number))));
		}});
		UnresolvedOperator op = new UnresolvedOperator(pos, "[]");
		FunctionDefinition fn = new FunctionDefinition(FunctionName.function(pos, null, "[]"), 0, null);
		op.bind(fn);
		NumericLiteral e1 = new NumericLiteral(pos, "42", 2);
		ApplyExpr ae = new ApplyExpr(pos, op, e1);
		aec.result(new ExprResult(pos, LoadBuiltins.nil)); // the "operator" for any list is always Nil
		aec.result(new ExprResult(pos, LoadBuiltins.number));
		aec.leaveApplyExpr(ae);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void consOfATwoStringsReturnsAPolyInstanceOfString() {
		ApplyExpressionChecker aec = new ApplyExpressionChecker(errors, repository, state, nv, false);
		context.checking(new Expectations() {{
			oneOf(state).consolidate(pos, Arrays.asList(new PosType(pos, LoadBuiltins.string), new PosType(pos, LoadBuiltins.string))); will(returnValue(new PosType(pos, LoadBuiltins.string)));
			oneOf(nv).result(with(PolyInstanceMatcher.of(LoadBuiltins.cons, Matchers.is(LoadBuiltins.string))));
		}});
		UnresolvedOperator op = new UnresolvedOperator(pos, "[]");
		FunctionDefinition fn = new FunctionDefinition(FunctionName.function(pos, null, "[]"), 0, null);
		op.bind(fn);
		NumericLiteral e1 = new NumericLiteral(pos, "42", 2);
		ApplyExpr ae = new ApplyExpr(pos, op, e1);
		aec.result(new ExprResult(pos, LoadBuiltins.nil)); // the "operator" for any list is always Nil
		aec.result(new ExprResult(pos, LoadBuiltins.string));
		aec.result(new ExprResult(pos, LoadBuiltins.string));
		aec.leaveApplyExpr(ae);
	}
}
