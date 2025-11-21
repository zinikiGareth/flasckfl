package test.parsing;

import java.net.URI;
import java.util.ArrayList;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.errors.LocalErrorTracker;
import org.flasck.flas.grammar.tracking.LoggableToken;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.parser.FunctionGuardedEquationConsumer;
import org.flasck.flas.parser.LastOneOnlyNestedParser;
import org.flasck.flas.parser.LocationTracker;
import org.flasck.flas.parser.TDAFunctionGuardedEquationParser;
import org.flasck.flas.testsupport.matchers.FunctionCaseDefnMatcher;
import org.flasck.flas.tokenizers.Tokenizable;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.zinutils.support.jmock.ReturnInvoker;

public class TDAFunctionGuardedCaseParsingTests {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	private ErrorReporter errorsMock = context.mock(ErrorReporter.class);
	private ErrorReporter errors = new LocalErrorTracker(errorsMock);
	private FunctionGuardedEquationConsumer consumer = context.mock(FunctionGuardedEquationConsumer.class);
	private URI fred = URI.create("file:/fred");
	private InputPosition pos = new InputPosition(fred, 1, 0, null, null);
	private TDAFunctionGuardedEquationParser parser;
	private FunctionIntro intro = new FunctionIntro(FunctionName.function(pos, null, "f"), new ArrayList<>());

	@Before
	public void setup() {
		LastOneOnlyNestedParser loonp = context.mock(LastOneOnlyNestedParser.class);
		context.checking(new Expectations() {{
			allowing(loonp).anotherParent();
			allowing(loonp).bindLocationTracker(with(any(LocationTracker.class)));
			allowing(errorsMock).logParsingToken(with(any(LoggableToken.class))); will(ReturnInvoker.arg(0));
			allowing(errorsMock).logReduction(with(any(String.class)), with(any(InputPosition.class)), with(any(InputPosition.class)));
		}});
		parser = new TDAFunctionGuardedEquationParser(errors, intro, pos, consumer, loonp, null);
	}

	@Test
	public void itsLegalToJustHaveADefaultCaseThusSimplyIdentingTheExpression() {
		context.checking(new Expectations() {{
			oneOf(consumer).functionCase(with(FunctionCaseDefnMatcher.isDefault()));
		}});
		parser.tryParsing(TDAFunctionParsingTests.line("= 42"));
		parser.scopeComplete(pos);
	}

	@Test
	public void itsNotLegalToHaveNoCases() {
		context.checking(new Expectations() {{
			oneOf(errorsMock).message(pos, "no function cases specified");
			oneOf(consumer).breakIt();
		}});
		parser.scopeComplete(pos);
	}

	@Test
	public void itsLegalToJustHaveASingleGuardedCase() {
		context.checking(new Expectations() {{
			oneOf(consumer).functionCase(with(FunctionCaseDefnMatcher.isGuarded()));
		}});
		parser.tryParsing(TDAFunctionParsingTests.line("| true = 42"));
		parser.scopeComplete(pos);
	}

	@Test
	public void obviouslyOneGuardAndOneDefaultAreFine() {
		context.checking(new Expectations() {{
			oneOf(consumer).functionCase(with(FunctionCaseDefnMatcher.isGuarded()));
			oneOf(consumer).functionCase(with(FunctionCaseDefnMatcher.isDefault()));
		}});
		parser.tryParsing(TDAFunctionParsingTests.line("| x == 10 = 42"));
		parser.tryParsing(TDAFunctionParsingTests.line("= 42"));
		parser.scopeComplete(pos);
	}

	@Test
	public void manyGuardsAndOneDefaultAreFine() {
		context.checking(new Expectations() {{
			exactly(3).of(consumer).functionCase(with(FunctionCaseDefnMatcher.isGuarded()));
			oneOf(consumer).functionCase(with(FunctionCaseDefnMatcher.isDefault()));
		}});
		parser.tryParsing(TDAFunctionParsingTests.line("| x == 10 = 42"));
		parser.tryParsing(TDAFunctionParsingTests.line("| x == 14 = 42"));
		parser.tryParsing(TDAFunctionParsingTests.line("| x == 18 = 42"));
		parser.tryParsing(TDAFunctionParsingTests.line("= 42"));
		parser.scopeComplete(pos);
	}

	@Test
	public void multipleDefaultCasesAreNotAllowed() {
		final Tokenizable secondCase = TDAFunctionParsingTests.line("= 42");
		context.checking(new Expectations() {{
			oneOf(consumer).functionCase(with(FunctionCaseDefnMatcher.isDefault()));
			oneOf(errorsMock).message(secondCase.realinfo(), "default case has already been specified");
		}});
		parser.tryParsing(TDAFunctionParsingTests.line("= 42"));
		parser.tryParsing(secondCase);
		parser.scopeComplete(pos);
	}

	@Test
	public void defaultCaseMustBeLastCase() {
		final Tokenizable secondCase = TDAFunctionParsingTests.line("| x == 10 = 42");
		context.checking(new Expectations() {{
			oneOf(consumer).functionCase(with(FunctionCaseDefnMatcher.isDefault()));
			oneOf(errorsMock).message(secondCase.realinfo(), "default case has already been specified");
		}});
		parser.tryParsing(TDAFunctionParsingTests.line("= 42"));
		parser.tryParsing(secondCase);
		parser.scopeComplete(pos);
	}

	@Test
	public void dontKeepOnAboutIt() {
		final Tokenizable secondCase = TDAFunctionParsingTests.line("= 42");
		context.checking(new Expectations() {{
			oneOf(consumer).functionCase(with(FunctionCaseDefnMatcher.isDefault()));
			oneOf(errorsMock).message(secondCase.realinfo(), "default case has already been specified");
		}});
		parser.tryParsing(TDAFunctionParsingTests.line("= 42"));
		parser.tryParsing(secondCase);
		parser.tryParsing(TDAFunctionParsingTests.line("= 42"));
		parser.scopeComplete(pos);
	}
}

