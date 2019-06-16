package test.parsing;

import static org.junit.Assert.assertTrue;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parser.LastOneOnlyNestedParser;
import org.flasck.flas.parser.PackageNamer;
import org.flasck.flas.parser.TDAParsing;
import org.flasck.flas.parser.TopLevelDefinitionConsumer;
import org.flasck.flas.parser.TopLevelNamer;
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
	private TopLevelNamer functionNamer = new PackageNamer("test.pkg");
	private TopLevelDefinitionConsumer builder = context.mock(TopLevelDefinitionConsumer.class);
	private InputPosition pos = new InputPosition("-", 1, 0, "hello");
	private TDAParsing parser;

	@Before
	public void setup() {
		parser = TDAMultiParser.topLevelUnit(tracker, functionNamer, builder);
	}
	
	@Test
	public void weCanHaveTwoFunctionsInTheSameScope() {
		context.checking(new Expectations() {{
			oneOf(builder).functionDefn(with(any(FunctionDefinition.class)));
			oneOf(builder).functionDefn(with(any(FunctionDefinition.class)));
		}});
		parser.tryParsing(line("f = 42"));
		parser.tryParsing(line("g = 86"));
		parser.scopeComplete(pos);
	}

	@Test
	public void errorsFromPatternsShouldntCascade() {
		final Tokenizable line = line("f (T T) = 42");
		context.checking(new Expectations() {{
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
			oneOf(builder).functionDefn(with(any(FunctionDefinition.class)));
			oneOf(builder).functionDefn(with(any(FunctionDefinition.class)));
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


	// These want to go somewhere else where they fit, like NESTING tests
	@Test
	public void aNestedScopeIsLegalAsLongAsItComesAtTheEnd() {
		context.checking(new Expectations() {{
//			oneOf(consumer).functionCase(with(FunctionCaseDefnMatcher.isGuarded()));
//			oneOf(consumer).functionCase(with(FunctionCaseDefnMatcher.isDefault()));
//			oneOf(topLevel).functionCase(with(FunctionCaseDefnMatcher.isDefault()));
		}});
		TDAParsing guards = parser.tryParsing(line("f"));
		guards.tryParsing(TDAFunctionParsingTests.line("| x == 10 = 42"));
		TDAParsing nested = guards.tryParsing(TDAFunctionParsingTests.line("= 42"));
		assertTrue(nested instanceof LastOneOnlyNestedParser);
		nested.tryParsing(TDAFunctionParsingTests.line("g = 'hello'"));
		nested.scopeComplete(pos);
		guards.scopeComplete(pos);
	}

	@Test
	public void aNestedScopeIsLegalAsLongAsItComesAtTheEndEvenWithNoDefault() {
		context.checking(new Expectations() {{
//			oneOf(consumer).functionCase(with(FunctionCaseDefnMatcher.isGuarded()));
//			oneOf(consumer).functionCase(with(FunctionCaseDefnMatcher.isGuarded()));
//			oneOf(topLevel).functionCase(with(FunctionCaseDefnMatcher.isDefault()));
		}});
		TDAParsing guards = parser.tryParsing(line("f"));
		guards.tryParsing(TDAFunctionParsingTests.line("| x == 10 = 42"));
		TDAParsing nested = guards.tryParsing(TDAFunctionParsingTests.line("| x == 12 = 42"));
		assertTrue(nested instanceof LastOneOnlyNestedParser);
		nested.tryParsing(TDAFunctionParsingTests.line("g = 'hello'"));
		nested.scopeComplete(pos);
		guards.scopeComplete(pos);
	}

	@Test
	public void aNestedScopeIsNotLegalBeforeTheFinalCase() {
		final Tokenizable nestedLine = TDAFunctionParsingTests.line("g = 'hello'");
		context.checking(new Expectations() {{
//			oneOf(consumer).functionCase(with(FunctionCaseDefnMatcher.isGuarded()));
//			oneOf(consumer).functionCase(with(FunctionCaseDefnMatcher.isDefault()));
//			oneOf(topLevel).functionCase(with(FunctionCaseDefnMatcher.isDefault()));
			oneOf(errors).message(nestedLine, "nested scope must be after last case");
		}});
		TDAParsing guards = parser.tryParsing(line("f"));
		TDAParsing nested = guards.tryParsing(TDAFunctionParsingTests.line("| x == 10 = 42"));
		assertTrue(nested instanceof LastOneOnlyNestedParser);
		nested.tryParsing(nestedLine);
		nested.scopeComplete(pos);
		guards.tryParsing(TDAFunctionParsingTests.line("= 42"));
		guards.scopeComplete(pos);
	}

	public static Tokenizable line(String string) {
		return new Tokenizable(TDAStoryTests.line(string));
	}

}