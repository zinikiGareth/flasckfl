package test.parsing;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.blocker.TDAParsingWithAction;
import org.flasck.flas.commonBase.Locatable;
import org.flasck.flas.compiler.ParsingPhase;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.errors.LocalErrorTracker;
import org.flasck.flas.grammar.tracking.LoggableToken;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parser.LastOneOnlyNestedParser;
import org.flasck.flas.parser.PackageNamer;
import org.flasck.flas.parser.TDAParsing;
import org.flasck.flas.parser.TopLevelDefinitionConsumer;
import org.flasck.flas.parser.TopLevelNamer;
import org.flasck.flas.tokenizers.Tokenizable;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.zinutils.support.jmock.ReturnInvoker;

import flas.matchers.FunctionDefinitionMatcher;
import test.flas.stories.TDAStoryTests;

public class TDAFunctionParsingNestingTests {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	private ErrorReporter errors = context.mock(ErrorReporter.class);
	private ErrorReporter tracker = new LocalErrorTracker(errors);
	private TopLevelNamer functionNamer = new PackageNamer("test.pkg");
	private TopLevelDefinitionConsumer builder = context.mock(TopLevelDefinitionConsumer.class);
	private InputPosition pos = new InputPosition("-", 1, 0, null, "hello");
	private TDAParsing parser;

	@Before
	public void setup() {
		parser = ParsingPhase.topLevelUnit(tracker, functionNamer, builder, new ArrayList<>());
		context.checking(new Expectations() {{
			allowing(errors).logParsingToken(with(any(LoggableToken.class))); will(ReturnInvoker.arg(0));
			allowing(errors).logReduction(with(any(String.class)), with(any(InputPosition.class)), with(any(InputPosition.class)));
			allowing(errors).logReduction(with(any(String.class)), with(any(Locatable.class)), with(any(Locatable.class)));
		}});
	}

	@Test
	public void weCanHaveTwoFunctionsInTheSameScope() {
		context.checking(new Expectations() {{
			oneOf(builder).functionDefn(with(tracker), with(any(FunctionDefinition.class)));
			oneOf(builder).functionDefn(with(tracker), with(any(FunctionDefinition.class)));
		}});
		parser.tryParsing(line("f = 42"));
		parser.tryParsing(line("g = 86"));
		parser.scopeComplete(pos);
	}

	@Test
	public void errorsFromPatternsShouldntCascade() {
		final Tokenizable line = line("f (T T) = 42");
		context.checking(new Expectations() {{
//			oneOf(builder).polytype(with(tracker), with(any(PolyType.class)));
			oneOf(errors).message(line, "invalid pattern");
//			oneOf(builder).functionCase(with(any(FunctionCaseDefn.class)));
//			oneOf(functionNamer).functionName(with(any(InputPosition.class)), with("g")); will(returnValue(FunctionName.function(pos, null, "g")));
//			oneOf(builder).functionCase(with(any(FunctionCaseDefn.class)));
		}});
		TDAParsing parser = ParsingPhase.topLevelUnit(tracker, functionNamer, builder, new ArrayList<>());
		parser.tryParsing(line);
		parser.scopeComplete(pos);
	}

	@Test
	public void weCanHaveTwoFunctionsWithGuardsInTheSameScope() {
		context.checking(new Expectations() {{
			oneOf(builder).functionDefn(with(tracker), with(any(FunctionDefinition.class)));
			oneOf(builder).functionDefn(with(tracker), with(any(FunctionDefinition.class)));
		}});
		TDAParsing parser = ParsingPhase.topLevelUnit(tracker, functionNamer, builder, new ArrayList<>());
		TDAParsing nested = parser.tryParsing(line("f"));
		nested.tryParsing(line("| true = 42"));
		nested.scopeComplete(pos);
		nested = parser.tryParsing(line("g"));
		nested.tryParsing(line("| false = 86"));
		nested.scopeComplete(pos);
		parser.scopeComplete(pos);
	}


	@Test
	public void aNestedScopeIsLegalAsLongAsItComesAtTheEnd() {
		context.checking(new Expectations() {{
			oneOf(builder).functionDefn(with(tracker), with(FunctionDefinitionMatcher.named("test.pkg.f._1.g")));
		}});
		TDAParsing guards = parser.tryParsing(line("f"));
		guards.tryParsing(TDAFunctionParsingTests.line("| x == 10 = 42"));
		TDAParsing nested = guards.tryParsing(TDAFunctionParsingTests.line("= 42"));
		assertTrue(nested instanceof TDAParsingWithAction);
		assertTrue(((TDAParsingWithAction)nested).parser instanceof LastOneOnlyNestedParser);
		nested.tryParsing(TDAFunctionParsingTests.line("g = 'hello'"));
		nested.scopeComplete(pos);
		guards.scopeComplete(pos);
	}

	@Test
	public void aNestedScopeIsLegalAsLongAsItComesAtTheEndEvenWithNoDefault() {
		context.checking(new Expectations() {{
			oneOf(builder).functionDefn(with(tracker), with(FunctionDefinitionMatcher.named("test.pkg.f._1.g")));
		}});
		TDAParsing guards = parser.tryParsing(line("f"));
		guards.tryParsing(TDAFunctionParsingTests.line("| x == 10 = 42"));
		TDAParsing nested = guards.tryParsing(TDAFunctionParsingTests.line("| x == 12 = 42"));
		assertTrue(nested instanceof TDAParsingWithAction);
		assertTrue(((TDAParsingWithAction)nested).parser instanceof LastOneOnlyNestedParser);
		nested.tryParsing(TDAFunctionParsingTests.line("g = 'hello'"));
		nested.scopeComplete(pos);
		guards.scopeComplete(pos);
	}

	@Test
	public void aNestedScopeIsNotLegalBeforeTheFinalCase() {
		final Tokenizable nestedLine = TDAFunctionParsingTests.line("g = 'hello'");
		context.checking(new Expectations() {{
			oneOf(builder).functionDefn(with(tracker), with(FunctionDefinitionMatcher.named("test.pkg.f._1.g")));
			oneOf(errors).message(nestedLine.realinfo(), "nested scope must be after last case");
		}});
		TDAParsing guards = parser.tryParsing(line("f"));
		TDAParsing nested = guards.tryParsing(TDAFunctionParsingTests.line("| x == 10 = 42"));
		assertTrue(nested instanceof TDAParsingWithAction);
		assertTrue(((TDAParsingWithAction)nested).parser instanceof LastOneOnlyNestedParser);
		nested.tryParsing(nestedLine);
		nested.scopeComplete(pos);
		guards.tryParsing(TDAFunctionParsingTests.line("= 42"));
		guards.scopeComplete(pos);
	}

	public static Tokenizable line(String string) {
		return new Tokenizable(TDAStoryTests.line(string));
	}

}
