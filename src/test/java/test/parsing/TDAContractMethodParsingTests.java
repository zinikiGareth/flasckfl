package test.parsing;

import static org.junit.Assert.assertTrue;

import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parser.ContractMethodConsumer;
import org.flasck.flas.parser.ContractMethodParser;
import org.flasck.flas.parser.IgnoreNestedParser;
import org.flasck.flas.parser.NoNestingParser;
import org.flasck.flas.parser.TDAParsing;
import org.flasck.flas.tokenizers.Tokenizable;
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
			allowing(errors).hasErrors(); will(returnValue(false));
			oneOf(builder).addMethod(with(ContractMethodMatcher.up("fred")));
		}});
		ContractMethodParser parser = new ContractMethodParser(errors, builder);
		TDAParsing nested = parser.tryParsing(TDABasicIntroParsingTests.line("up fred"));
		assertTrue(nested instanceof NoNestingParser);
	}

	@Test
	public void aSimpleDownMethod() {
		context.checking(new Expectations() {{
			allowing(errors).hasErrors(); will(returnValue(false));
			oneOf(builder).addMethod(with(ContractMethodMatcher.down("fred")));
		}});
		ContractMethodParser parser = new ContractMethodParser(errors, builder);
		TDAParsing nested = parser.tryParsing(TDABasicIntroParsingTests.line("down fred"));
		assertTrue(nested instanceof NoNestingParser);
	}

	@Test
	public void sidewayIsNotADirection() {
		context.checking(new Expectations() {{
			oneOf(errors).message(with(any(Tokenizable.class)), with("missing or invalid direction"));
		}});
		ContractMethodParser parser = new ContractMethodParser(errors, builder);
		TDAParsing nested = parser.tryParsing(TDABasicIntroParsingTests.line("sideways fred"));
		assertTrue(nested instanceof IgnoreNestedParser);
	}

	@Test
	public void aDirectionIsRequired() {
		context.checking(new Expectations() {{
			oneOf(errors).message(with(any(Tokenizable.class)), with("missing or invalid direction"));
		}});
		ContractMethodParser parser = new ContractMethodParser(errors, builder);
		TDAParsing nested = parser.tryParsing(TDABasicIntroParsingTests.line("fred"));
		assertTrue(nested instanceof IgnoreNestedParser);
	}

	@Test
	public void methodsMayBeDeclaredOptional() {
		context.checking(new Expectations() {{
			allowing(errors).hasErrors(); will(returnValue(false));
			oneOf(builder).addMethod(with(ContractMethodMatcher.up("fred").optional()));
		}});
		ContractMethodParser parser = new ContractMethodParser(errors, builder);
		TDAParsing nested = parser.tryParsing(TDABasicIntroParsingTests.line("optional up fred"));
		assertTrue(nested instanceof NoNestingParser);
	}

	@Test
	public void methodMayHaveOneSimpleArgument() {
		context.checking(new Expectations() {{
			allowing(errors).hasErrors(); will(returnValue(false));
			oneOf(builder).addMethod(with(ContractMethodMatcher.up("fred").arg(VarPatternMatcher.var("x"))));
		}});
		ContractMethodParser parser = new ContractMethodParser(errors, builder);
		TDAParsing nested = parser.tryParsing(TDABasicIntroParsingTests.line("up fred x"));
		assertTrue(nested instanceof NoNestingParser);
	}
}
