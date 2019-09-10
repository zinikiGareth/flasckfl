package test.tc3;

import static org.junit.Assert.assertEquals;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.ApplyExpr;
import org.flasck.flas.commonBase.NumericLiteral;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.UnresolvedOperator;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.repository.RepositoryReader;
import org.flasck.flas.tc3.ApplyExpressionChecker;
import org.flasck.flas.tc3.ExpressionChecker;
import org.flasck.flas.tc3.Type;
import org.flasck.flas.tc3.TypeChecker;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Rule;
import org.junit.Test;

public class StackVisitation {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	private InputPosition pos = new InputPosition("-", 1, 0, "hello");
	private ErrorReporter errors = context.mock(ErrorReporter.class);
	private RepositoryReader repository = context.mock(RepositoryReader.class);
	private NestedVisitor nv = context.mock(NestedVisitor.class);

	@Test
	public void whenWeVisitAFunctionWePushAnExpressionMatcher() {
		FunctionDefinition fn = new FunctionDefinition(FunctionName.function(pos, null, "f"), 0);
		context.checking(new Expectations() {{
			oneOf(nv).push(with(any(TypeChecker.class)));
			oneOf(nv).push(with(any(ExpressionChecker.class)));
		}});
		TypeChecker tc = new TypeChecker(errors, repository, nv);
		tc.visitFunction(fn);
	}

	@Test
	public void theResultIsWhatWeEndUpSpittingOut() {
		FunctionDefinition fn = new FunctionDefinition(FunctionName.function(pos, null, "f"), 0);
		Type ty = context.mock(Type.class);
		context.checking(new Expectations() {{
			oneOf(nv).push(with(any(TypeChecker.class)));
			oneOf(nv).push(with(any(ExpressionChecker.class)));
		}});
		TypeChecker tc = new TypeChecker(errors, repository, nv);
		tc.visitFunction(fn);
		tc.result(ty);
		tc.leaveFunction(fn);
		assertEquals(ty, fn.type());
	}

	@Test
	public void applyExpressionsPushAnotherMatcher() {
		ExpressionChecker ec = new ExpressionChecker(repository, nv);
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
		ApplyExpressionChecker aec = new ApplyExpressionChecker(repository, nv);
		context.checking(new Expectations() {{
			oneOf(nv).push(with(any(ExpressionChecker.class)));
		}});
		UnresolvedOperator op = new UnresolvedOperator(pos, "+");
		aec.visitExpr(op, 2);
	}

	@Test
	public void leaveApplyExpressionWithValidTypesReturnsResult() {
		ApplyExpressionChecker aec = new ApplyExpressionChecker(repository, nv);
		Type fnt = context.mock(Type.class, "fn/2");
		Type nbr = context.mock(Type.class, "nbr");
		context.checking(new Expectations() {{
			oneOf(fnt).argCount(); will(returnValue(2));
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

}
