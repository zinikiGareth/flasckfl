package test.parsing;

import static org.junit.Assert.assertTrue;

import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.StateDefinition;
import org.flasck.flas.parser.ObjectElementsConsumer;
import org.flasck.flas.parser.TDAObjectElementsParser;
import org.flasck.flas.parser.TDAParsing;
import org.flasck.flas.parser.TDAStructFieldParser;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class TDAObjectElementParsingTests {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	private ErrorReporter errors = context.mock(ErrorReporter.class);
	private ObjectElementsConsumer builder = context.mock(ObjectElementsConsumer.class);

	@Before
	public void setup() {
		context.checking(new Expectations() {{
//			allowing(builder).scopeTo(with(any(ScopeReceiver.class)));
		}});
	}

	@Test
	public void objectsCanHaveAStateParser() {
		context.checking(new Expectations() {{
//			allowing(builder).qualifyName("Store"); will(returnValue(new SolidName(null, "Store")));
			oneOf(builder).defineState(with(any(StateDefinition.class)));
		}});
		TDAObjectElementsParser parser = new TDAObjectElementsParser(errors, builder);
		TDAParsing nested = parser.tryParsing(TDABasicIntroParsingTests.line("state"));
		assertTrue(nested instanceof TDAStructFieldParser);
	}
	
}
