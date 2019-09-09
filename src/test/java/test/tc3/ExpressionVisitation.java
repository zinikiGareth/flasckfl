package test.tc3;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.NumericLiteral;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.repository.RepositoryEntry;
import org.flasck.flas.repository.RepositoryReader;
import org.flasck.flas.tc3.ExpressionChecker;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Rule;
import org.junit.Test;

public class ExpressionVisitation {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	private InputPosition pos = new InputPosition("-", 1, 0, "hello");

	@Test
	public void whenWeVisitAFunctionWePushAnExpressionMatcher() {
		RepositoryReader repository = context.mock(RepositoryReader.class);
		NestedVisitor nv = context.mock(NestedVisitor.class);
		RepositoryEntry tyNumber = context.mock(RepositoryEntry.class);
		context.checking(new Expectations() {{
			oneOf(repository).get("Number"); will(returnValue(tyNumber));
			oneOf(nv).result(tyNumber);
		}});
		ExpressionChecker tc = new ExpressionChecker(repository, nv);
		tc.visitNumericLiteral(new NumericLiteral(pos, "42", 2));
	}
}
