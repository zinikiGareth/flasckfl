package test.parsing;

import static org.junit.Assert.assertTrue;

import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parser.ContractMethodConsumer;
import org.flasck.flas.parser.ContractMethodParser;
import org.flasck.flas.parser.NoNestingParser;
import org.flasck.flas.parser.TDAParsing;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Rule;
import org.junit.Test;

public class TDAContractMethodParsingTests {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	private ErrorReporter errors = context.mock(ErrorReporter.class);
	private ContractMethodConsumer builder = context.mock(ContractMethodConsumer.class);

	@Test
	public void aSimpleUpMethod() {
		context.checking(new Expectations() {{
			oneOf(builder).addMethod(with(ContractMethodMatcher.up("fred")));
		}});
		ContractMethodParser parser = new ContractMethodParser(errors, builder);
		TDAParsing nested = parser.tryParsing(TDABasicIntroParsingTests.line("up fred"));
		assertTrue(nested instanceof NoNestingParser);
	}
}
