package test.parsing;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.compiler.ScopeReceiver;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.FunctionCaseDefn;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.parser.FunctionNameProvider;
import org.flasck.flas.parser.LocalErrorTracker;
import org.flasck.flas.parser.TDAParsing;
import org.flasck.flas.parser.TopLevelDefinitionConsumer;
import org.flasck.flas.stories.TDAMultiParser;
import org.flasck.flas.tokenizers.Tokenizable;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import test.flas.stories.TDAStoryTests;

public class TDAFunctionParsingNestingTests {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	private ErrorReporter errors = context.mock(ErrorReporter.class);
	private ErrorReporter tracker = new LocalErrorTracker(errors);
	private FunctionNameProvider functionNamer = context.mock(FunctionNameProvider.class);
	private TopLevelDefinitionConsumer builder = context.mock(TopLevelDefinitionConsumer.class);
	private InputPosition pos = new InputPosition("-", 1, 0, "hello");

	@Before
	public void setup() {
		context.checking(new Expectations() {{
			allowing(builder).scopeTo(with(any(ScopeReceiver.class)));
		}});
	}

	@Test
	public void weCanHaveTwoFunctionsInTheSameScope() {
		context.checking(new Expectations() {{
			oneOf(functionNamer).functionName(with(any(InputPosition.class)), with("f")); will(returnValue(FunctionName.function(pos, null, "f")));
			oneOf(builder).functionCase(with(any(FunctionCaseDefn.class)));
			oneOf(functionNamer).functionName(with(any(InputPosition.class)), with("g")); will(returnValue(FunctionName.function(pos, null, "g")));
			oneOf(builder).functionCase(with(any(FunctionCaseDefn.class)));
		}});
		TDAParsing parser = TDAMultiParser.topLevelUnit(tracker, functionNamer, builder);
		parser.tryParsing(line("f = 42"));
		parser.tryParsing(line("g = 86"));
		parser.scopeComplete(pos);
	}

	@Test
	public void errorsFromPatternsShouldntCascade() {
		final Tokenizable line = line("f (T T) = 42");
		context.checking(new Expectations() {{
			oneOf(functionNamer).functionName(with(any(InputPosition.class)), with("f")); will(returnValue(FunctionName.function(pos, null, "f")));
			oneOf(errors).message(line, "invalid pattern");
//			oneOf(builder).functionCase(with(any(FunctionCaseDefn.class)));
//			oneOf(functionNamer).functionName(with(any(InputPosition.class)), with("g")); will(returnValue(FunctionName.function(pos, null, "g")));
//			oneOf(builder).functionCase(with(any(FunctionCaseDefn.class)));
		}});
		TDAParsing parser = TDAMultiParser.topLevelUnit(tracker, functionNamer, builder);
		parser.tryParsing(line);
		parser.scopeComplete(pos);
	}

	@Test
	public void weCanHaveTwoFunctionsWithGuardsInTheSameScope() {
		context.checking(new Expectations() {{
			oneOf(functionNamer).functionName(with(any(InputPosition.class)), with("f")); will(returnValue(FunctionName.function(pos, null, "f")));
			oneOf(builder).functionIntro(with(any(FunctionIntro.class)));
			oneOf(builder).functionCase(with(any(FunctionCaseDefn.class)));
			oneOf(functionNamer).functionName(with(any(InputPosition.class)), with("g")); will(returnValue(FunctionName.function(pos, null, "g")));
			oneOf(builder).functionIntro(with(any(FunctionIntro.class)));
			oneOf(builder).functionCase(with(any(FunctionCaseDefn.class)));
		}});
		TDAParsing parser = TDAMultiParser.topLevelUnit(tracker, functionNamer, builder);
		TDAParsing nested = parser.tryParsing(line("f"));
		nested.tryParsing(line("| true = 42"));
		nested.scopeComplete(pos);
		nested = parser.tryParsing(line("g"));
		nested.tryParsing(line("| false = 86"));
		nested.scopeComplete(pos);
		parser.scopeComplete(pos);
	}

	public static Tokenizable line(String string) {
		return new Tokenizable(TDAStoryTests.line(string));
	}

}
