package test.parsing;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.net.URI;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.blocker.TDAParsingWithAction;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.errors.LocalErrorTracker;
import org.flasck.flas.grammar.tracking.LoggableToken;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.parsedForm.TypedPattern;
import org.flasck.flas.parsedForm.VarPattern;
import org.flasck.flas.parser.FunctionCaseNameProvider;
import org.flasck.flas.parser.FunctionIntroConsumer;
import org.flasck.flas.parser.FunctionNameProvider;
import org.flasck.flas.parser.IgnoreNestedParser;
import org.flasck.flas.parser.TDAFunctionGuardedEquationParser;
import org.flasck.flas.parser.TDAFunctionParser;
import org.flasck.flas.parser.TDAParsing;
import org.flasck.flas.parser.TopLevelDefinitionConsumer;
import org.flasck.flas.stories.TDAMultiParser;
import org.flasck.flas.testsupport.TestSupport;
import org.flasck.flas.testsupport.matchers.TypedPatternMatcher;
import org.flasck.flas.testsupport.matchers.VarPatternMatcher;
import org.flasck.flas.tokenizers.Tokenizable;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.zinutils.support.jmock.ReturnInvoker;

public class TDAFunctionParsingTests {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	private ErrorReporter errors = context.mock(ErrorReporter.class);
	private ErrorReporter tracker = new LocalErrorTracker(errors);
	private FunctionNameProvider functionNamer = context.mock(FunctionNameProvider.class, "functionNamer");
	private FunctionCaseNameProvider caseNamer = context.mock(FunctionCaseNameProvider.class, "caseNamer");
	private FunctionIntroConsumer intro = context.mock(FunctionIntroConsumer.class);
	private TopLevelDefinitionConsumer builder = context.mock(TopLevelDefinitionConsumer.class);
	private URI fred = URI.create("file:/fred");
	private InputPosition pos = new InputPosition(fred, 1, 0, null, null);
	private PackageName pkg = new PackageName("test.pkg");

	@Before
	public void ignoreParserLogging() {
		context.checking(new Expectations() {{
			allowing(errors).logParsingToken(with(any(LoggableToken.class))); will(ReturnInvoker.arg(0));
			allowing(errors).logReduction(with(any(String.class)), with(any(InputPosition.class)), with(any(InputPosition.class)));
		}});
	}

	@Test
	public void aBlankLineReturnsNothingAndDoesNothing() {
		context.checking(new Expectations() {{
			oneOf(intro).moveOn();
		}});
		TDAFunctionParser parser = new TDAFunctionParser(tracker, functionNamer, caseNamer, intro, builder, null, null);
		TDAParsing nested = parser.tryParsing(line(""));
		assertNull(nested);
	}

	@Test
	public void justANameIsAFunctionIntroWithNestedCaseParser() {
		final Tokenizable line = line("f");
		FunctionName nameF = FunctionName.function(pos, pkg, "f");
		context.checking(new Expectations() {{
			oneOf(functionNamer).functionName(with(any(InputPosition.class)), with("f")); will(returnValue(nameF));
			oneOf(intro).nextCaseNumber(nameF); will(returnValue(1));
			oneOf(caseNamer).functionCaseName(with(any(InputPosition.class)), with("f"), with(1)); will(returnValue(FunctionName.caseName(nameF, 1)));
			oneOf(intro).functionIntro(with(any(FunctionIntro.class)));
			oneOf(intro).hasGuards(true);
		}});
		TDAFunctionParser parser = new TDAFunctionParser(tracker, functionNamer, caseNamer, intro, builder, null, null);
		TDAParsing nested = parser.tryParsing(line);
		assertNotNull(nested);
		assertTrue(TDAParsingWithAction.is(nested, TDAFunctionGuardedEquationParser.class));
	}

	@Test
	public void aNameMustHaveAnindentedCaseParserThatSeesSomething() {
		final Tokenizable line = line("f");
		FunctionName nameF = FunctionName.function(pos, pkg, "f");
		context.checking(new Expectations() {{
			oneOf(functionNamer).functionName(with(any(InputPosition.class)), with("f")); will(returnValue(nameF));
			oneOf(intro).nextCaseNumber(nameF); will(returnValue(1));
			oneOf(caseNamer).functionCaseName(with(any(InputPosition.class)), with("f"), with(1)); will(returnValue(FunctionName.caseName(nameF, 1)));
			oneOf(intro).functionIntro(with(any(FunctionIntro.class)));
			oneOf(intro).hasGuards(true);
			oneOf(errors).message(with(any(InputPosition.class)), with("no function cases specified")	);
		}});
		TDAFunctionParser parser = new TDAFunctionParser(tracker, functionNamer, caseNamer, intro, builder, null, null);
		TDAParsing nested = parser.tryParsing(line);
		nested.scopeComplete(line.realinfo());
	}

	@Test
	public void aNameCanTakeTheArgumentsFirst() {
		final Tokenizable line = line("f x");
		FunctionName nameF = FunctionName.function(pos, pkg, "f");
		context.checking(new Expectations() {{
			oneOf(functionNamer).functionName(with(any(InputPosition.class)), with("f")); will(returnValue(nameF));
			oneOf(intro).nextCaseNumber(nameF); will(returnValue(1));
			oneOf(caseNamer).functionCaseName(with(any(InputPosition.class)), with("f"), with(1)); will(returnValue(FunctionName.caseName(nameF, 1)));
			oneOf(builder).argument(with(aNull(ErrorReporter.class)), (VarPattern) with(VarPatternMatcher.var("test.pkg.f._1.x")));
			oneOf(intro).functionIntro(with(any(FunctionIntro.class)));
			oneOf(intro).hasGuards(true);
			oneOf(errors).message(with(any(InputPosition.class)), with("no function cases specified")	);
		}});
		TDAFunctionParser parser = new TDAFunctionParser(tracker, functionNamer, caseNamer, intro, builder, null, null);
		TDAParsing nested = parser.tryParsing(line);
		nested.scopeComplete(line.realinfo());
	}

	@Test
	public void aFunctionDeclCannotEndAtTheEquals() {
		final Tokenizable line = line("f = ");
		FunctionName nameF = FunctionName.function(pos, pkg, "f");
		context.checking(new Expectations() {{
			oneOf(functionNamer).functionName(with(any(InputPosition.class)), with("f")); will(returnValue(nameF));
			oneOf(intro).nextCaseNumber(nameF); will(returnValue(1));
			oneOf(caseNamer).functionCaseName(with(any(InputPosition.class)), with("f"), with(1)); will(returnValue(FunctionName.caseName(nameF, 1)));
			oneOf(intro).functionIntro(with(any(FunctionIntro.class)));
			oneOf(errors).message(line, "function definition requires expression");
		}});
		TDAFunctionParser parser = new TDAFunctionParser(tracker, functionNamer, caseNamer, intro, builder, null, null);
		TDAParsing nested = parser.tryParsing(line);
		assertTrue(nested instanceof IgnoreNestedParser);
	}

	@Test
	public void aFullFunctionDefinitionReturnsATopLevelParserOfSorts() {
		FunctionName nameF = FunctionName.function(pos, pkg, "f");
		context.checking(new Expectations() {{
			oneOf(functionNamer).functionName(with(any(InputPosition.class)), with("f")); will(returnValue(nameF));
			oneOf(intro).nextCaseNumber(nameF); will(returnValue(1));
			oneOf(caseNamer).functionCaseName(with(any(InputPosition.class)), with("f"), with(1)); will(returnValue(FunctionName.caseName(nameF, 1)));
			oneOf(intro).functionIntro(with(any(FunctionIntro.class)));
			oneOf(intro).hasGuards(false);
		}});
		TDAFunctionParser parser = new TDAFunctionParser(tracker, functionNamer, caseNamer, intro, builder, null, null);
		TDAParsing nested = parser.tryParsing(line("f = 3"));
		assertNotNull(nested);
		assertTrue(TDAParsingWithAction.is(nested, TDAMultiParser.class));
	}

	@Test
	public void aFunctionDefinitionCanHaveAVariableArg() {
		FunctionName nameF = FunctionName.function(pos, pkg, "f");
		context.checking(new Expectations() {{
			oneOf(functionNamer).functionName(with(any(InputPosition.class)), with("f")); will(returnValue(nameF));
			oneOf(intro).nextCaseNumber(nameF); will(returnValue(1));
			oneOf(caseNamer).functionCaseName(with(any(InputPosition.class)), with("f"), with(1)); will(returnValue(FunctionName.caseName(nameF, 1)));
			oneOf(intro).functionIntro(with(any(FunctionIntro.class)));
			oneOf(builder).argument(with(aNull(ErrorReporter.class)), (VarPattern) with(VarPatternMatcher.var("test.pkg.f._1.x")));
			oneOf(intro).hasGuards(false);
		}});
		TDAFunctionParser parser = new TDAFunctionParser(tracker, functionNamer, caseNamer, intro, builder, null, null);
		TDAParsing nested = parser.tryParsing(line("f x = 3"));
		assertNotNull(nested);
		assertTrue(TDAParsingWithAction.is(nested, TDAMultiParser.class));
	}

	@Test
	public void aFunctionDefinitionCanHaveATypedArg() {
		FunctionName nameF = FunctionName.function(pos, pkg, "f");
		context.checking(new Expectations() {{
			oneOf(functionNamer).functionName(with(any(InputPosition.class)), with("f")); will(returnValue(nameF));
			oneOf(intro).nextCaseNumber(nameF); will(returnValue(1));
			oneOf(caseNamer).functionCaseName(with(any(InputPosition.class)), with("f"), with(1)); will(returnValue(FunctionName.caseName(nameF, 1)));
			oneOf(intro).functionIntro(with(any(FunctionIntro.class)));
			oneOf(builder).argument(with(tracker), (TypedPattern) with(TypedPatternMatcher.typed("Number", "n")));
			oneOf(intro).hasGuards(false);
		}});
		TDAFunctionParser parser = new TDAFunctionParser(tracker, functionNamer, caseNamer, intro, builder, null, null);
		TDAParsing nested = parser.tryParsing(line("f (Number n) = n"));
		assertNotNull(nested);
		assertTrue(TDAParsingWithAction.is(nested, TDAMultiParser.class));
	}

	public static Tokenizable line(String string) {
		return new Tokenizable(TestSupport.line(string));
	}

}
