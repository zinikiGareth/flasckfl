package test.parsing;

import static org.junit.Assert.assertTrue;

import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.StateDefinition;
import org.flasck.flas.parser.IgnoreNestedParser;
import org.flasck.flas.parser.ObjectElementsConsumer;
import org.flasck.flas.parser.TDAObjectElementsParser;
import org.flasck.flas.parser.TDAParsing;
import org.flasck.flas.parser.TDAStructFieldParser;
import org.flasck.flas.tokenizers.Tokenizable;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Rule;
import org.junit.Test;

public class TDAObjectElementParsingTests {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	private ErrorReporter errorsMock = context.mock(ErrorReporter.class);
	private ObjectElementsConsumer builder = context.mock(ObjectElementsConsumer.class);

	@Test
	public void objectsCanHaveAStateParser() {
		context.checking(new Expectations() {{
			oneOf(builder).defineState(with(any(StateDefinition.class)));
		}});
		TDAObjectElementsParser parser = new TDAObjectElementsParser(errorsMock, builder);
		TDAParsing nested = parser.tryParsing(TDABasicIntroParsingTests.line("state"));
		assertTrue(nested instanceof TDAStructFieldParser);
	}
	
	@Test
	public void objectStateCannotHaveANameOrAnything() {
		context.checking(new Expectations() {{
			oneOf(errorsMock).message(with(any(Tokenizable.class)), with("extra characters at end of line"));
		}});
		TDAObjectElementsParser parser = new TDAObjectElementsParser(errorsMock, builder);
		TDAParsing nested = parser.tryParsing(TDABasicIntroParsingTests.line("state Fred"));
		assertTrue(nested instanceof IgnoreNestedParser);
	}
	
}
