package test.tc3;

import static org.junit.Assert.assertEquals;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.WithTypeSignature;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.repository.RepositoryReader;
import org.flasck.flas.tc3.ExpressionChecker;
import org.flasck.flas.tc3.TypeChecker;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Rule;
import org.junit.Test;

public class StackVisitation {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	private InputPosition pos = new InputPosition("-", 1, 0, "hello");

	@Test
	public void whenWeVisitAFunctionWePushAnExpressionMatcher() {
		ErrorReporter errors = context.mock(ErrorReporter.class);
		RepositoryReader repository = context.mock(RepositoryReader.class);
		NestedVisitor nv = context.mock(NestedVisitor.class);
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
		ErrorReporter errors = context.mock(ErrorReporter.class);
		RepositoryReader repository = context.mock(RepositoryReader.class);
		NestedVisitor nv = context.mock(NestedVisitor.class);
		FunctionDefinition fn = new FunctionDefinition(FunctionName.function(pos, null, "f"), 0);
		WithTypeSignature wts = context.mock(WithTypeSignature.class);
		context.checking(new Expectations() {{
			oneOf(nv).push(with(any(TypeChecker.class)));
			oneOf(nv).push(with(any(ExpressionChecker.class)));
		}});
		TypeChecker tc = new TypeChecker(errors, repository, nv);
		tc.visitFunction(fn);
		tc.result(wts);
		tc.leaveFunction(fn);
		assertEquals(wts, fn.type());
	}
}
