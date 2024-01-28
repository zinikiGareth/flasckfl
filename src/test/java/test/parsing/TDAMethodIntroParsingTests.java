package test.parsing;

import static org.junit.Assert.assertTrue;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.blocker.TDAParsingWithAction;
import org.flasck.flas.commonBase.Locatable;
import org.flasck.flas.errors.ErrorMark;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.grammar.tracking.LoggableToken;
import org.flasck.flas.parsedForm.StandaloneMethod;
import org.flasck.flas.parsedForm.VarPattern;
import org.flasck.flas.parser.PackageNamer;
import org.flasck.flas.parser.TDAIntroParser;
import org.flasck.flas.parser.TDAMethodMessageParser;
import org.flasck.flas.parser.TDAParsing;
import org.flasck.flas.parser.TopLevelDefinitionConsumer;
import org.flasck.flas.parser.TopLevelNamer;
import org.flasck.flas.testsupport.TestSupport;
import org.flasck.flas.testsupport.matchers.VarPatternMatcher;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.zinutils.support.jmock.ReturnInvoker;

public class TDAMethodIntroParsingTests {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	private ErrorReporter errors = context.mock(ErrorReporter.class);
	private TopLevelDefinitionConsumer builder = context.mock(TopLevelDefinitionConsumer.class);
	private TopLevelNamer namer = new PackageNamer("test.pkg");

	@Before
	public void ignoreParserLogging() {
		context.checking(new Expectations() {{
			allowing(errors).logParsingToken(with(any(LoggableToken.class))); will(ReturnInvoker.arg(0));
			allowing(errors).logReduction(with(any(String.class)), with(any(InputPosition.class)), with(any(InputPosition.class)));
			allowing(errors).logReduction(with(any(String.class)), with(any(Locatable.class)), with(any(Locatable.class)));
		}});
	}

	@Test
	public void aStandaloneMethodCanBeDefined() {
		ErrorMark mark = context.mock(ErrorMark.class);
		context.checking(new Expectations() {{
			allowing(errors).hasErrors(); will(returnValue(false));
			allowing(errors).mark(); will(returnValue(mark));
			allowing(mark).hasMoreNow(); will(returnValue(false));
			oneOf(builder).newStandaloneMethod(with(errors), with(any(StandaloneMethod.class)));
		}});
		TDAIntroParser parser = new TDAIntroParser(errors, namer, builder);
		TDAParsing nested = parser.tryParsing(TestSupport.tokline("method foo"));
		assertTrue(TDAParsingWithAction.is(nested, TDAMethodMessageParser.class));
	}

	@Test
	public void aStandaloneMethodCanHaveAnArgument() {
		ErrorMark mark = context.mock(ErrorMark.class);
		context.checking(new Expectations() {{
			allowing(errors).hasErrors(); will(returnValue(false));
			allowing(errors).mark(); will(returnValue(mark));
			allowing(mark).hasMoreNow(); will(returnValue(false));
			oneOf(builder).newStandaloneMethod(with(errors), with(any(StandaloneMethod.class)));
			oneOf(builder).argument(with(aNull(ErrorReporter.class)), (VarPattern) with(VarPatternMatcher.var("test.pkg.foo.x")));
		}});
		TDAIntroParser parser = new TDAIntroParser(errors, namer, builder);
		TDAParsing nested = parser.tryParsing(TestSupport.tokline("method foo x"));
		assertTrue(TDAParsingWithAction.is(nested, TDAMethodMessageParser.class));
	}

	// TODO: error cases
}
