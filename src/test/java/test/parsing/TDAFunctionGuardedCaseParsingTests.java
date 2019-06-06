package test.parsing;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.compiler.ScopeReceiver;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.parser.FunctionIntroConsumer;
import org.flasck.flas.parser.FunctionNameProvider;
import org.flasck.flas.parser.LocalErrorTracker;
import org.flasck.flas.parser.TDAFunctionCaseParser;
import org.flasck.flas.parser.TDAFunctionParser;
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
	private TopLevelDefinitionConsumer builder = context.mock(TopLevelDefinitionConsumer.class);
	private InputPosition pos = new InputPosition("-", 1, 0, "hello");
	private TDAFunctionCaseParser parser;

	@Before
	public void setup() {
		final Tokenizable line = TDAFunctionParsingTests.line("f");
		FunctionNameProvider topNamer = context.mock(FunctionNameProvider.class);
		context.checking(new Expectations() {{
			allowing(builder).scopeTo(with(any(ScopeReceiver.class)));
			oneOf(topNamer).functionName(with(any(InputPosition.class)), with("f")); will(returnValue(FunctionName.function(pos, null, "f")));
			oneOf(intro).functionIntro(with(any(FunctionIntro.class)));
		}});
		TDAFunctionParser mainFn = new TDAFunctionParser(errors, topNamer, intro, builder);
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

	// many cases
	// not more than one default case
	// default case must come last
	// nested scope must come at end
}

