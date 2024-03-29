package test.parsing;

import static org.junit.Assert.assertNull;

import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.grammar.tracking.LoggableToken;
import org.flasck.flas.parser.TDAIntroParser;
import org.flasck.flas.parser.TDAParsing;
import org.flasck.flas.parser.TopLevelDefinitionConsumer;
import org.flasck.flas.parser.TopLevelNamer;
import org.flasck.flas.testsupport.TestSupport;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.zinutils.support.jmock.ReturnInvoker;

public class TDABasicIntroParsingTests {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	private ErrorReporter errors = context.mock(ErrorReporter.class);
	private TopLevelDefinitionConsumer builder = context.mock(TopLevelDefinitionConsumer.class);
	// TODO: might be easier (and better) to use the real thing (a PackageNamer)
	private TopLevelNamer namer = context.mock(TopLevelNamer.class);

	@Before
	public void ignoreParserLogging() {
		context.checking(new Expectations() {{
			allowing(errors).logParsingToken(with(any(LoggableToken.class))); will(ReturnInvoker.arg(0));
		}});
	}

	@Test
	public void aBlankLineReturnsNothingAndDoesNothing() {
		context.checking(new Expectations() {{
		}});
		TDAIntroParser parser = new TDAIntroParser(errors, namer, builder);
		TDAParsing nested = parser.tryParsing(TestSupport.tokline(""));
		assertNull(nested);
	}

	@Test
	public void aLineWithoutAPlausibleKeywordDoesNothing() {
		context.checking(new Expectations() {{
		}});
		TDAIntroParser parser = new TDAIntroParser(errors, namer, builder);
		TDAParsing nested = parser.tryParsing(TestSupport.tokline("+x"));
		assertNull(nested);
	}

	@Test
	public void aLineWithAnUnrecognizedKeywordDoesNothing() {
		context.checking(new Expectations() {{
		}});
		TDAIntroParser parser = new TDAIntroParser(errors, namer, builder);
		TDAParsing nested = parser.tryParsing(TestSupport.tokline("garbage"));
		assertNull(nested);
	}
}
