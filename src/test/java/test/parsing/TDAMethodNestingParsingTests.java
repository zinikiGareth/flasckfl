package test.parsing;

import static org.junit.Assert.assertEquals;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.blocker.TDAParsingWithAction;
import org.flasck.flas.commonBase.Locatable;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.commonBase.names.SolidName;
import org.flasck.flas.errors.ErrorMark;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.errors.LocalErrorTracker;
import org.flasck.flas.grammar.tracking.LoggableToken;
import org.flasck.flas.parsedForm.ObjectActionHandler;
import org.flasck.flas.parsedForm.ObjectCtor;
import org.flasck.flas.parsedForm.StateHolder;
import org.flasck.flas.parser.LocationTracker;
import org.flasck.flas.parser.ObjectElementsConsumer;
import org.flasck.flas.parser.ObjectNestedNamer;
import org.flasck.flas.parser.TDAMethodMessageParser;
import org.flasck.flas.parser.TDAObjectElementsParser;
import org.flasck.flas.parser.TDAParsing;
import org.flasck.flas.parser.TopLevelDefinitionConsumer;
import org.flasck.flas.testsupport.TestSupport;
import org.flasck.flas.tokenizers.Tokenizable;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.zinutils.support.jmock.CaptureAction;
import org.zinutils.support.jmock.ReturnInvoker;

/** The way in which methods nest is non-obvious (but I think the same is true of guarded equations)
 * At the top (block) level is the method declaration (with or without the method keyword, depending on context)
 * Inside this are the method actions
 * Only the last of these can have a further level of nesting, which is a function scope.
 */
public class TDAMethodNestingParsingTests {
	interface SHBuilder extends ObjectElementsConsumer, StateHolder {}
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	private ErrorReporter errors = context.mock(ErrorReporter.class);
	private ErrorReporter tracker = new LocalErrorTracker(errors);
	private ObjectElementsConsumer builder = context.mock(SHBuilder.class);
	private SolidName name = new SolidName(new PackageName("hello"), "MyObject");
	private ObjectNestedNamer namer = new ObjectNestedNamer(name);
	private TopLevelDefinitionConsumer topLevel = context.mock(TopLevelDefinitionConsumer.class);
	private LocationTracker locTracker = null;
	
	@Before
	public void ignoreParserLogging() {
		context.checking(new Expectations() {{
			allowing(errors).logParsingToken(with(any(LoggableToken.class))); will(ReturnInvoker.arg(0));
			allowing(errors).logReduction(with(any(String.class)), with(any(InputPosition.class)), with(any(InputPosition.class)));
			allowing(errors).logReduction(with(any(String.class)), with(any(Locatable.class)), with(any(Locatable.class)));
		}});
	}

	@Test
	public void anObjectCtorCanHaveActionsWithNoNesting() {
		CaptureAction captureIt = new CaptureAction(null);
		ErrorMark mark = context.mock(ErrorMark.class);
		context.checking(new Expectations() {{
			allowing(errors).hasErrors(); will(returnValue(false));
			allowing(errors).mark(); will(returnValue(mark));
			allowing(mark).hasMoreNow(); will(returnValue(false));
			oneOf(builder).addConstructor(with(any(ObjectCtor.class))); will(captureIt);
		}});
		TDAObjectElementsParser oep = new TDAObjectElementsParser(errors, namer, builder, topLevel, locTracker);
		TDAParsing pwa = oep.tryParsing(TestSupport.tokline("ctor testMe"));
		TDAMethodMessageParser nested = (TDAMethodMessageParser) ((TDAParsingWithAction)pwa).parser;
		nested.tryParsing(TestSupport.tokline("<- ds.getReady"));
		nested.tryParsing(TestSupport.tokline("x <- 'hello'"));
		final ObjectActionHandler ctor = (ObjectActionHandler) captureIt.get(0);
		assertEquals(2, ctor.messages().size());
	}

	@Test
	public void anObjectCtorCanHaveNestedScopeOnTheFinalAction() {
		context.checking(new Expectations() {{
			allowing(errors).hasErrors(); will(returnValue(false));
			oneOf(builder).addConstructor(with(any(ObjectCtor.class)));
//			oneOf(topLevel).functionCase(with(FunctionCaseMatcher.called(name, "s")));
		}});
		TDAObjectElementsParser oep = new TDAObjectElementsParser(tracker, namer, builder, topLevel, locTracker);
		TDAParsingWithAction paw = (TDAParsingWithAction) oep.tryParsing(TestSupport.tokline("ctor testMe"));
		TDAMethodMessageParser nested = (TDAMethodMessageParser) paw.parser;
		nested.tryParsing(TestSupport.tokline("<- ds.send y"));
		TDAParsing fsParser = nested.tryParsing(TestSupport.tokline("x <- y"));
		fsParser.tryParsing(TestSupport.tokline("s = 'hello'"));
	}

	@Test
	public void anActionCannotHaveANestedScopeIfItIsNotTheLastOne() {
		// extract this here since we want to check it's the one that errors, but we only supply it later ...
		final Tokenizable line = TestSupport.tokline("s = 'hello'");
		context.checking(new Expectations() {{
			allowing(errors).hasErrors(); will(returnValue(false));
			oneOf(builder).addConstructor(with(any(ObjectCtor.class)));
//			oneOf(topLevel).functionCase(with(FunctionCaseMatcher.called(name, "s")));
			oneOf(errors).message(line.realinfo(), "nested scope must be after last action");
		}});
		TDAObjectElementsParser oep = new TDAObjectElementsParser(tracker, namer, builder, topLevel, locTracker);
		TDAParsing pwa = oep.tryParsing(TestSupport.tokline("ctor testMe"));
		TDAMethodMessageParser nested = (TDAMethodMessageParser) ((TDAParsingWithAction)pwa).parser;
		TDAParsing fsParser = nested.tryParsing(TestSupport.tokline("<- ds.send y"));
		fsParser.tryParsing(line);
		nested.tryParsing(TestSupport.tokline("x <- y"));
	}

	@Test
	public void checkWeDontGetMultipleMessagesForTheSameOffence() {
		// extract this here since we want to check it's the one that errors, but we only supply it later ...
		final Tokenizable line = TestSupport.tokline("s = 'hello'");
		context.checking(new Expectations() {{
			allowing(errors).hasErrors(); will(returnValue(false));
			oneOf(builder).addConstructor(with(any(ObjectCtor.class)));
//			oneOf(topLevel).functionCase(with(FunctionCaseMatcher.called(name, "s")));
			oneOf(errors).message(line.realinfo(), "nested scope must be after last action");
		}});
		TDAObjectElementsParser oep = new TDAObjectElementsParser(tracker, namer, builder, topLevel, locTracker);
		TDAParsing pwa = oep.tryParsing(TestSupport.tokline("ctor testMe"));
		TDAMethodMessageParser nested = (TDAMethodMessageParser) ((TDAParsingWithAction)pwa).parser;
		TDAParsing fsParser = nested.tryParsing(TestSupport.tokline("<- ds.send y"));
		fsParser.tryParsing(line);
		nested.tryParsing(TestSupport.tokline("x <- y"));
		nested.tryParsing(TestSupport.tokline("v <- 42"));
	}

	@Test
	public void cannotDirectlyNestScopeUnderMethodWithoutActions() {
		// extract this here since we want to check it's the one that errors, but we only supply it later ...
		final Tokenizable line = TestSupport.tokline("s = 'hello'");
		ErrorMark mark = context.mock(ErrorMark.class);
		context.checking(new Expectations() {{
			allowing(errors).hasErrors(); will(returnValue(false));
			allowing(errors).mark(); will(returnValue(mark));
			allowing(mark).hasMoreNow(); will(returnValue(false));
			oneOf(builder).addConstructor(with(any(ObjectCtor.class)));
			oneOf(errors).message(with(any(InputPosition.class)), with("expected <-"));
		}});
		TDAObjectElementsParser oep = new TDAObjectElementsParser(errors, namer, builder, topLevel, locTracker);
		TDAParsingWithAction paw = (TDAParsingWithAction) oep.tryParsing(TestSupport.tokline("ctor testMe"));
		((TDAMethodMessageParser) paw.parser).tryParsing(line);
	}
}
