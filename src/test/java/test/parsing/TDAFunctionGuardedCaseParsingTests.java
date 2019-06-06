package test.parsing;

import static org.junit.Assert.assertTrue;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.compiler.ScopeReceiver;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.parser.FunctionIntroConsumer;
import org.flasck.flas.parser.FunctionNameProvider;
import org.flasck.flas.parser.LastOneOnlyNestedParser;
import org.flasck.flas.parser.LocalErrorTracker;
import org.flasck.flas.parser.TDAFunctionCaseParser;
import org.flasck.flas.parser.TDAFunctionParser;
import org.flasck.flas.parser.TDAParsing;
import org.flasck.flas.parser.TopLevelDefinitionConsumer;
import org.flasck.flas.tokenizers.Tokenizable;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class TDAFunctionGuardedCaseParsingTests {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	private ErrorReporter errorsMock = context.mock(ErrorReporter.class);
	private ErrorReporter errors = new LocalErrorTracker(errorsMock);
	private FunctionIntroConsumer intro = context.mock(FunctionIntroConsumer.class);
	private TopLevelDefinitionConsumer topLevel = context.mock(TopLevelDefinitionConsumer.class);
	private InputPosition pos = new InputPosition("-", 1, 0, "hello");
	private TDAFunctionCaseParser parser;

	@Before
	public void setup() {
		final Tokenizable line = TDAFunctionParsingTests.line("f");
		FunctionNameProvider topNamer = context.mock(FunctionNameProvider.class);
		context.checking(new Expectations() {{
			allowing(topLevel).scopeTo(with(any(ScopeReceiver.class)));
			oneOf(topNamer).functionName(with(any(InputPosition.class)), with("f")); will(returnValue(FunctionName.function(pos, null, "f")));
			oneOf(intro).functionIntro(with(any(FunctionIntro.class)));
		}});
		TDAFunctionParser mainFn = new TDAFunctionParser(errors, topNamer, intro, topLevel);
		parser = (TDAFunctionCaseParser) mainFn.tryParsing(line);
	}

	@Test
	public void itsLegalToJustHaveADefaultCaseThusSimplyIdentingTheExpression() {
		context.checking(new Expectations() {{
			oneOf(intro).functionCase(with(FunctionCaseDefnMatcher.isDefault()));
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
			oneOf(intro).functionCase(with(FunctionCaseDefnMatcher.isGuarded()));
		}});
		parser.tryParsing(TDAFunctionParsingTests.line("| true = 42"));
		parser.scopeComplete(pos);
	}

	@Test
	public void obviouslyOneGuardAndOneDefaultAreFine() {
		context.checking(new Expectations() {{
			oneOf(intro).functionCase(with(FunctionCaseDefnMatcher.isGuarded()));
			oneOf(intro).functionCase(with(FunctionCaseDefnMatcher.isDefault()));
		}});
		parser.tryParsing(TDAFunctionParsingTests.line("| x == 10 = 42"));
		parser.tryParsing(TDAFunctionParsingTests.line("= 42"));
		parser.scopeComplete(pos);
	}

	@Test
	public void manyGuardsAndOneDefaultAreFine() {
		context.checking(new Expectations() {{
			exactly(3).of(intro).functionCase(with(FunctionCaseDefnMatcher.isGuarded()));
			oneOf(intro).functionCase(with(FunctionCaseDefnMatcher.isDefault()));
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
			oneOf(intro).functionCase(with(FunctionCaseDefnMatcher.isDefault()));
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
			oneOf(intro).functionCase(with(FunctionCaseDefnMatcher.isDefault()));
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
			oneOf(intro).functionCase(with(FunctionCaseDefnMatcher.isDefault()));
			oneOf(errorsMock).message(secondCase.realinfo(), "default case has already been specified");
		}});
		parser.tryParsing(TDAFunctionParsingTests.line("= 42"));
		parser.tryParsing(secondCase);
		parser.tryParsing(TDAFunctionParsingTests.line("= 42"));
		parser.scopeComplete(pos);
	}

	@Test
	public void aNestedScopeIsLegalAsLongAsItComesAtTheEnd() {
		context.checking(new Expectations() {{
			oneOf(intro).functionCase(with(FunctionCaseDefnMatcher.isGuarded()));
			oneOf(intro).functionCase(with(FunctionCaseDefnMatcher.isDefault()));
			oneOf(topLevel).functionCase(with(FunctionCaseDefnMatcher.isDefault()));
		}});
		parser.tryParsing(TDAFunctionParsingTests.line("| x == 10 = 42"));
		TDAParsing nested = parser.tryParsing(TDAFunctionParsingTests.line("= 42"));
		assertTrue(nested instanceof LastOneOnlyNestedParser);
		nested.tryParsing(TDAFunctionParsingTests.line("g = 'hello'"));
		nested.scopeComplete(pos);
		parser.scopeComplete(pos);
	}

	@Test
	public void aNestedScopeIsLegalAsLongAsItComesAtTheEndEvenWithNoDefault() {
		context.checking(new Expectations() {{
			oneOf(intro).functionCase(with(FunctionCaseDefnMatcher.isGuarded()));
			oneOf(intro).functionCase(with(FunctionCaseDefnMatcher.isGuarded()));
			oneOf(topLevel).functionCase(with(FunctionCaseDefnMatcher.isDefault()));
		}});
		parser.tryParsing(TDAFunctionParsingTests.line("| x == 10 = 42"));
		TDAParsing nested = parser.tryParsing(TDAFunctionParsingTests.line("| x == 12 = 42"));
		assertTrue(nested instanceof LastOneOnlyNestedParser);
		nested.tryParsing(TDAFunctionParsingTests.line("g = 'hello'"));
		nested.scopeComplete(pos);
		parser.scopeComplete(pos);
	}

	@Test
	public void aNestedScopeIsNotLegalBeforeTheFinalCase() {
		final Tokenizable nestedLine = TDAFunctionParsingTests.line("g = 'hello'");
		context.checking(new Expectations() {{
			oneOf(intro).functionCase(with(FunctionCaseDefnMatcher.isGuarded()));
			oneOf(intro).functionCase(with(FunctionCaseDefnMatcher.isDefault()));
			oneOf(topLevel).functionCase(with(FunctionCaseDefnMatcher.isDefault()));
			oneOf(errorsMock).message(nestedLine, "nested scope must be after last case");
		}});
		TDAParsing nested = parser.tryParsing(TDAFunctionParsingTests.line("| x == 10 = 42"));
		assertTrue(nested instanceof LastOneOnlyNestedParser);
		nested.tryParsing(nestedLine);
		nested.scopeComplete(pos);
		parser.tryParsing(TDAFunctionParsingTests.line("= 42"));
		parser.scopeComplete(pos);
	}

}

