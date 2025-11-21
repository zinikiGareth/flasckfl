package test.parsing;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.net.URI;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.blocker.TDAParsingWithAction;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.errors.LocalErrorTracker;
import org.flasck.flas.grammar.tracking.LoggableToken;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parser.IgnoreNestedParser;
import org.flasck.flas.parser.PackageNamer;
import org.flasck.flas.parser.TDAIntroParser;
import org.flasck.flas.parser.TDAParsing;
import org.flasck.flas.parser.TopLevelDefinitionConsumer;
import org.flasck.flas.parser.TopLevelNamer;
import org.flasck.flas.stories.TDAMultiParser;
import org.flasck.flas.testsupport.TestSupport;
import org.flasck.flas.testsupport.matchers.HandlerImplementsMatcher;
import org.flasck.flas.testsupport.matchers.ObjectDefnMatcher;
import org.flasck.flas.testsupport.matchers.TokenizableMatcher;
import org.flasck.flas.tokenizers.Tokenizable;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.zinutils.support.jmock.ReturnInvoker;

public class TDAObjectIntroParsingTests {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	private ErrorReporter errors = context.mock(ErrorReporter.class);
	private LocalErrorTracker tracker = new LocalErrorTracker(errors);
	private TopLevelDefinitionConsumer builder = context.mock(TopLevelDefinitionConsumer.class);
	private TopLevelNamer namer = new PackageNamer("test.pkg");
	private URI fred = URI.create("file:/fred");
	private InputPosition pos = new InputPosition(fred, 1, 0, null, null);

	@Before
	public void ignoreParserLogging() {
		context.checking(new Expectations() {{
			allowing(errors).logParsingToken(with(any(LoggableToken.class))); will(ReturnInvoker.arg(0));
			allowing(errors).logReduction(with(any(String.class)), with(any(InputPosition.class)), with(any(InputPosition.class)));
		}});
	}

	@Test
	public void theSimplestObjectCreatesAScopeEntryAndReturnsAFieldParser() {
		context.checking(new Expectations() {{
			oneOf(builder).newObject(with(errors), with(ObjectDefnMatcher.match("test.pkg.Store")));
		}});
		TDAIntroParser parser = new TDAIntroParser(errors, namer, builder);
		TDAParsing nested = parser.tryParsing(TestSupport.tokline("object Store"));
		assertTrue(TDAParsingWithAction.is(nested, TDAMultiParser.class));
	}

	@Test
	public void anObjectCanIncludeAFunction() {
		context.checking(new Expectations() {{
			oneOf(builder).newObject(with(tracker), with(ObjectDefnMatcher.match("test.pkg.Store")));
			oneOf(builder).functionDefn(with(tracker), with(any(FunctionDefinition.class)));
		}});
		TDAIntroParser parser = new TDAIntroParser(tracker, namer, builder);
		TDAParsing nested = parser.tryParsing(TestSupport.tokline("object Store"));
		assertTrue(TDAParsingWithAction.is(nested, TDAMultiParser.class));
		nested.tryParsing(TestSupport.tokline("ctor obligatory"));
		nested.tryParsing(TestSupport.tokline("f = 42"));
		nested.scopeComplete(pos);
	}
	
	@Test
	public void objectsCanHaveNestedHandlers() {
		context.checking(new Expectations() {{
			oneOf(builder).newObject(with(tracker), with(ObjectDefnMatcher.match("test.pkg.Store")));
			oneOf(builder).newHandler(with(tracker), with(HandlerImplementsMatcher.named("test.pkg.Store.Handler")));
		}});
		TDAIntroParser parser = new TDAIntroParser(tracker, namer, builder);
		TDAParsing nested = parser.tryParsing(TestSupport.tokline("object Store"));
		nested.tryParsing(TestSupport.tokline("handler Contract Handler"));
	}

	@Test
	public void thereMustBeATypeName() {
		Tokenizable toks = TestSupport.tokline("object");
		context.checking(new Expectations() {{
			oneOf(errors).message(toks, "invalid or missing type name");
		}});
		TDAIntroParser parser = new TDAIntroParser(errors, namer, builder);
		TDAParsing nested = parser.tryParsing(toks);
		assertNotNull(nested);
		assertTrue(nested instanceof IgnoreNestedParser);
	}

	@Test
	public void theTypeNameMustBeTheValidKind() {
		Tokenizable toks = TestSupport.tokline("object fred");
		context.checking(new Expectations() {{
			oneOf(errors).message(toks, "invalid or missing type name");
		}});
		TDAIntroParser parser = new TDAIntroParser(errors, namer, builder);
		TDAParsing nested = parser.tryParsing(toks);
		assertNotNull(nested);
		assertTrue(nested instanceof IgnoreNestedParser);
	}

	@Test
	public void aPolymorphicObjectDefinitionCreatesTheRightScopeEntryAndReturnsAFieldParser() {
		context.checking(new Expectations() {{
			oneOf(builder).newObject(with(errors), with(ObjectDefnMatcher.match("test.pkg.Store").poly("A").locs(0,7)));
		}});
		TDAIntroParser parser = new TDAIntroParser(errors, namer, builder);
		TDAParsing nested = parser.tryParsing(TestSupport.tokline("object Store A"));
		assertTrue(TDAParsingWithAction.is(nested, TDAMultiParser.class));
	}

	@Test
	public void aPolymorphicObjectDefinitionCannotBeInBrackets() {
		final Tokenizable toks = TestSupport.tokline("object Store[A]");
		context.checking(new Expectations() {{
			oneOf(errors).message(with(TokenizableMatcher.match("[A]")), with("syntax error"));
		}});
		TDAIntroParser parser = new TDAIntroParser(errors, namer, builder);
		TDAParsing nested = parser.tryParsing(toks);
		assertTrue(nested instanceof IgnoreNestedParser);
	}

	@Test
	public void polymorphicVarsMustBeValid() {
		final Tokenizable toks = TestSupport.tokline("object Store xx");
		context.checking(new Expectations() {{
			oneOf(errors).message(toks, "syntax error");
		}});
		TDAIntroParser parser = new TDAIntroParser(errors, namer, builder);
		TDAParsing nested = parser.tryParsing(toks);
		assertTrue(nested instanceof IgnoreNestedParser);
	}

	@Test
	public void objectsInPackagesHaveQualifiedNames() {
		context.checking(new Expectations() {{
			oneOf(builder).newObject(with(errors), with(ObjectDefnMatcher.match("test.pkg.InPackage")));
		}});
		TDAIntroParser parser = new TDAIntroParser(errors, namer, builder);
		TDAParsing nested = parser.tryParsing(TestSupport.tokline("object InPackage"));
		assertTrue(TDAParsingWithAction.is(nested, TDAMultiParser.class));
	}
}
