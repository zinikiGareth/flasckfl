package test.parsing;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.compiler.FLASCompiler;
import org.flasck.flas.compiler.ScopeReceiver;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.FunctionCaseDefn;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.parser.ParsedLineConsumer;
import org.flasck.flas.parser.TDAFunctionCaseParser;
import org.flasck.flas.parser.TDAFunctionParser;
import org.flasck.flas.parser.TDAParsing;
import org.flasck.flas.parser.TDATupleDeclarationParser;
import org.flasck.flas.stories.TDAMultiParser;
import org.flasck.flas.tokenizers.Tokenizable;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import test.flas.stories.TDAStoryTests;

public class TDAFunctionParsingTests {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	private ErrorReporter errors = context.mock(ErrorReporter.class);
	private ParsedLineConsumer builder = context.mock(ParsedLineConsumer.class);
	private InputPosition pos = new InputPosition("-", 1, 0, "hello");

	// TODO: remove this and delete BC version of parser when complete
	@BeforeClass
	public static void makeTDA() {
		FLASCompiler.backwardCompatibilityMode = false;
	}
	
	@Before
	public void setup() {
		context.checking(new Expectations() {{
			allowing(builder).scopeTo(with(any(ScopeReceiver.class)));
		}});
	}
	@Test
	public void aBlankLineReturnsNothingAndDoesNothing() {
		context.checking(new Expectations() {{
		}});
		TDAFunctionParser parser = new TDAFunctionParser(errors, builder);
		TDAParsing nested = parser.tryParsing(line(""));
		assertNull(nested);
	}

	@Test
	public void justANameIsAFunctionIntroWithNestedCaseParser() {
		final Tokenizable line = line("f");
		context.checking(new Expectations() {{
			oneOf(builder).functionName(with(any(InputPosition.class)), with("f")); will(returnValue(FunctionName.function(pos, null, "f")));
			oneOf(builder).functionIntro(with(any(FunctionIntro.class)));
		}});
		TDAFunctionParser parser = new TDAFunctionParser(errors, builder);
		TDAParsing nested = parser.tryParsing(line);
		assertNotNull(nested);
		assertTrue(nested instanceof TDAFunctionCaseParser);
	}

	@Test
	public void aFunctionDeclCannotEndAtTheEquals() {
		final Tokenizable line = line("f = ");
		context.checking(new Expectations() {{
			oneOf(builder).functionName(with(any(InputPosition.class)), with("f")); will(returnValue(FunctionName.function(pos, null, "f")));
			oneOf(errors).message(line, "function definition requires expression");
		}});
		TDAFunctionParser parser = new TDAFunctionParser(errors, builder);
		TDAParsing nested = parser.tryParsing(line);
		assertNull(nested);
	}

	@Test
	public void aFullFunctionDefinitionReturnsATopLevelParserOfSorts() {
		context.checking(new Expectations() {{
			oneOf(builder).functionName(with(any(InputPosition.class)), with("f")); will(returnValue(FunctionName.function(pos, null, "f")));
			oneOf(builder).functionCase(with(any(FunctionCaseDefn.class)));
		}});
		TDAFunctionParser parser = new TDAFunctionParser(errors, builder);
		TDAParsing nested = parser.tryParsing(line("f = 3"));
		assertNotNull(nested);
		assertTrue(nested instanceof TDAMultiParser);
	}

	@Test
	public void aFunctionDefinitionCanHaveAVariableArg() {
		context.checking(new Expectations() {{
			oneOf(builder).functionName(with(any(InputPosition.class)), with("f")); will(returnValue(FunctionName.function(pos, null, "f")));
			oneOf(builder).functionCase(with(FunctionCaseMatcher.called(null, "f")));
		}});
		TDAFunctionParser parser = new TDAFunctionParser(errors, builder);
		TDAParsing nested = parser.tryParsing(line("f x = 3"));
		assertNotNull(nested);
		assertTrue(nested instanceof TDAMultiParser);
	}

	public static Tokenizable line(String string) {
		return new Tokenizable(TDAStoryTests.line(string));
	}

}
