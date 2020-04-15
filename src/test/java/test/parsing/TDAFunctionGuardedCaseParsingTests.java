package test.parsing;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parser.FunctionGuardedEquationConsumer;
import org.flasck.flas.parser.LastOneOnlyNestedParser;
import org.flasck.flas.parser.TDAFunctionGuardedEquationParser;
import org.flasck.flas.tokenizers.Tokenizable;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import flas.matchers.FunctionCaseDefnMatcher;

public class TDAFunctionGuardedCaseParsingTests {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	private ErrorReporter errorsMock = context.mock(ErrorReporter.class);
	private ErrorReporter errors = new LocalErrorTracker(errorsMock);
	private FunctionGuardedEquationConsumer consumer = context.mock(FunctionGuardedEquationConsumer.class);
	private InputPosition pos = new InputPosition("-", 1, 0, "hello");
	private TDAFunctionGuardedEquationParser parser;

	@Before
	public void setup() {
		LastOneOnlyNestedParser loonp = context.mock(LastOneOnlyNestedParser.class);
		context.checking(new Expectations() {{
			allowing(loonp).anotherParent();
		}});
		parser = new TDAFunctionGuardedEquationParser(errors, pos, consumer, loonp);
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

