package test.parsing;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.parser.FunctionIntroConsumer;
import org.flasck.flas.parser.FunctionNameProvider;
import org.flasck.flas.parser.TDAFunctionGuardedEquationParser;
import org.flasck.flas.parser.TDAFunctionParser;
import org.flasck.flas.parser.TDAParsing;
import org.flasck.flas.parser.TopLevelDefinitionConsumer;
import org.flasck.flas.stories.TDAMultiParser;
import org.flasck.flas.tokenizers.Tokenizable;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Rule;
import org.junit.Test;

import test.flas.stories.TDAStoryTests;

public class TDAFunctionParsingTests {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	private ErrorReporter errors = context.mock(ErrorReporter.class);
	private ErrorReporter tracker = new LocalErrorTracker(errors);
	private FunctionNameProvider functionNamer = context.mock(FunctionNameProvider.class);
	private FunctionIntroConsumer intro = context.mock(FunctionIntroConsumer.class);
	private TopLevelDefinitionConsumer builder = context.mock(TopLevelDefinitionConsumer.class);
	private InputPosition pos = new InputPosition("-", 1, 0, "hello");

	@Test
	public void aBlankLineReturnsNothingAndDoesNothing() {
		context.checking(new Expectations() {{
		}});
		TDAFunctionParser parser = new TDAFunctionParser(tracker, functionNamer, intro, builder);
		TDAParsing nested = parser.tryParsing(line(""));
		assertNull(nested);
	}

	@Test
	public void justANameIsAFunctionIntroWithNestedCaseParser() {
		final Tokenizable line = line("f");
		context.checking(new Expectations() {{
			oneOf(functionNamer).functionName(with(any(InputPosition.class)), with("f")); will(returnValue(FunctionName.function(pos, null, "f")));
			oneOf(intro).functionIntro(with(any(FunctionIntro.class)));
		}});
		TDAFunctionParser parser = new TDAFunctionParser(tracker, functionNamer, intro, builder);
		TDAParsing nested = parser.tryParsing(line);
		assertNotNull(nested);
		assertTrue(nested instanceof TDAFunctionGuardedEquationParser);
	}

	@Test
	public void aNameMustHaveAnindentedCaseParserThatSeesSomething() {
		final Tokenizable line = line("f");
		context.checking(new Expectations() {{
			oneOf(functionNamer).functionName(with(any(InputPosition.class)), with("f")); will(returnValue(FunctionName.function(pos, null, "f")));
			oneOf(intro).functionIntro(with(any(FunctionIntro.class)));
			oneOf(errors).message(with(any(InputPosition.class)), with("no function cases specified")	);
		}});
		TDAFunctionParser parser = new TDAFunctionParser(tracker, functionNamer, intro, builder);
		TDAParsing nested = parser.tryParsing(line);
		nested.scopeComplete(line.realinfo());
	}

	@Test
	public void aFunctionDeclCannotEndAtTheEquals() {
		final Tokenizable line = line("f = ");
		context.checking(new Expectations() {{
			oneOf(functionNamer).functionName(with(any(InputPosition.class)), with("f")); will(returnValue(FunctionName.function(pos, null, "f")));
			oneOf(intro).functionIntro(with(any(FunctionIntro.class)));
			oneOf(errors).message(line, "function definition requires expression");
		}});
		TDAFunctionParser parser = new TDAFunctionParser(tracker, functionNamer, intro, builder);
		TDAParsing nested = parser.tryParsing(line);
		assertNull(nested);
	}

	@Test
	public void aFullFunctionDefinitionReturnsATopLevelParserOfSorts() {
		context.checking(new Expectations() {{
			oneOf(functionNamer).functionName(with(any(InputPosition.class)), with("f")); will(returnValue(FunctionName.function(pos, null, "f")));
			oneOf(intro).functionIntro(with(any(FunctionIntro.class)));
//			oneOf(guards).functionCase(with(FunctionCaseMatcher.called(null, "f")));
		}});
		TDAFunctionParser parser = new TDAFunctionParser(tracker, functionNamer, intro, builder);
		TDAParsing nested = parser.tryParsing(line("f = 3"));
		assertNotNull(nested);
		assertTrue(nested instanceof TDAMultiParser);
	}

	@Test
	public void aFunctionDefinitionCanHaveAVariableArg() {
		context.checking(new Expectations() {{
			oneOf(functionNamer).functionName(with(any(InputPosition.class)), with("f")); will(returnValue(FunctionName.function(pos, null, "f")));
			oneOf(intro).functionIntro(with(any(FunctionIntro.class)));
//			oneOf(guards).functionCase(with(FunctionCaseMatcher.called(null, "f").pattern(PatternMatcher.var("x"))));
		}});
		TDAFunctionParser parser = new TDAFunctionParser(tracker, functionNamer, intro, builder);
		TDAParsing nested = parser.tryParsing(line("f x = 3"));
		assertNotNull(nested);
		assertTrue(nested instanceof TDAMultiParser);
	}

	@Test
	public void aFunctionDefinitionCanHaveATypedArg() {
		context.checking(new Expectations() {{
			oneOf(functionNamer).functionName(with(any(InputPosition.class)), with("f")); will(returnValue(FunctionName.function(pos, null, "f")));
			oneOf(intro).functionIntro(with(any(FunctionIntro.class)));
//			oneOf(guards).functionCase(with(FunctionCaseMatcher.called(null, "f").pattern(PatternMatcher.typed("Number", "n"))));
		}});
		TDAFunctionParser parser = new TDAFunctionParser(tracker, functionNamer, intro, builder);
		TDAParsing nested = parser.tryParsing(line("f (Number n) = n"));
		assertNotNull(nested);
		assertTrue(nested instanceof TDAMultiParser);
	}

	public static Tokenizable line(String string) {
		return new Tokenizable(TDAStoryTests.line(string));
	}

}
