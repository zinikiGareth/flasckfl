package test.parsing;

import static org.junit.Assert.assertEquals;

import org.flasck.flas.commonBase.names.SolidName;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.ObjectCtor;
import org.flasck.flas.parser.FunctionScopeUnitConsumer;
import org.flasck.flas.parser.ObjectElementsConsumer;
import org.flasck.flas.parser.TDAMethodMessageParser;
import org.flasck.flas.parser.TDAObjectElementsParser;
import org.flasck.flas.parser.TDAParsing;
import org.flasck.flas.tokenizers.Tokenizable;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Rule;
import org.junit.Test;
import org.zinutils.support.jmock.CaptureAction;

/** The way in which methods nest is non-obvious (but I think the same is true of guarded equations)
 * At the top (block) level is the method declaration (with or without the method keyword, depending on context)
 * Inside this are the method actions
 * Only the last of these can have a further level of nesting, which is a function scope.
 */
public class TDAMethodNestingParsingTests {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	private ErrorReporter errors = context.mock(ErrorReporter.class);
	private ErrorReporter tracker = new LocalErrorTracker(errors);
	private ObjectElementsConsumer builder = context.mock(ObjectElementsConsumer.class);
	private SolidName name = new SolidName(null, "MyObject");
	private FunctionScopeUnitConsumer topLevel = context.mock(FunctionScopeUnitConsumer.class);
	
	@Test
	public void anObjectCtorCanHaveActionsWithNoNesting() {
		CaptureAction captureIt = new CaptureAction(null);
		context.checking(new Expectations() {{
			allowing(builder).name(); will(returnValue(name));
			allowing(errors).hasErrors(); will(returnValue(false));
			oneOf(builder).addConstructor(with(any(ObjectCtor.class))); will(captureIt);
		}});
		TDAObjectElementsParser oep = new TDAObjectElementsParser(errors, builder, topLevel);
		TDAMethodMessageParser nested = (TDAMethodMessageParser) oep.tryParsing(TDABasicIntroParsingTests.line("ctor testMe"));
		nested.tryParsing(TDABasicIntroParsingTests.line("<- ds.getReady"));
		nested.tryParsing(TDABasicIntroParsingTests.line("x <- 'hello'"));
		final ObjectCtor ctor = (ObjectCtor) captureIt.get(0);
		assertEquals(2, ctor.messages().size());
	}

	@Test
	public void anObjectCtorCanHaveNestedScopeOnTheFinalAction() {
		context.checking(new Expectations() {{
			allowing(builder).name(); will(returnValue(name));
			allowing(errors).hasErrors(); will(returnValue(false));
			oneOf(builder).addConstructor(with(any(ObjectCtor.class)));
			oneOf(topLevel).functionCase(with(FunctionCaseMatcher.called(name, "s")));
		}});
		TDAObjectElementsParser oep = new TDAObjectElementsParser(tracker, builder, topLevel);
		TDAMethodMessageParser nested = (TDAMethodMessageParser) oep.tryParsing(TDABasicIntroParsingTests.line("ctor testMe"));
		nested.tryParsing(TDABasicIntroParsingTests.line("<- ds.send y"));
		TDAParsing fsParser = nested.tryParsing(TDABasicIntroParsingTests.line("x <- y"));
		fsParser.tryParsing(TDABasicIntroParsingTests.line("s = 'hello'"));
	}

	@Test
	public void anActionCannotHaveANestedScopeIfItIsNotTheLastOne() {
		// extract this here since we want to check it's the one that errors, but we only supply it later ...
		final Tokenizable line = TDABasicIntroParsingTests.line("s = 'hello'");
		context.checking(new Expectations() {{
			allowing(builder).name(); will(returnValue(name));
			allowing(errors).hasErrors(); will(returnValue(false));
			oneOf(builder).addConstructor(with(any(ObjectCtor.class)));
			oneOf(topLevel).functionCase(with(FunctionCaseMatcher.called(name, "s")));
			oneOf(errors).message(line, "nested scope must be after last action");
		}});
		TDAObjectElementsParser oep = new TDAObjectElementsParser(tracker, builder, topLevel);
		TDAMethodMessageParser nested = (TDAMethodMessageParser) oep.tryParsing(TDABasicIntroParsingTests.line("ctor testMe"));
		TDAParsing fsParser = nested.tryParsing(TDABasicIntroParsingTests.line("<- ds.send y"));
		fsParser.tryParsing(line);
		nested.tryParsing(TDABasicIntroParsingTests.line("x <- y"));
	}

	@Test
	public void checkWeDontGetMultipleMessagesForTheSameOffence() {
		// extract this here since we want to check it's the one that errors, but we only supply it later ...
		final Tokenizable line = TDABasicIntroParsingTests.line("s = 'hello'");
		context.checking(new Expectations() {{
			allowing(builder).name(); will(returnValue(name));
			allowing(errors).hasErrors(); will(returnValue(false));
			oneOf(builder).addConstructor(with(any(ObjectCtor.class)));
			oneOf(topLevel).functionCase(with(FunctionCaseMatcher.called(name, "s")));
			oneOf(errors).message(line, "nested scope must be after last action");
		}});
		TDAObjectElementsParser oep = new TDAObjectElementsParser(tracker, builder, topLevel);
		TDAMethodMessageParser nested = (TDAMethodMessageParser) oep.tryParsing(TDABasicIntroParsingTests.line("ctor testMe"));
		TDAParsing fsParser = nested.tryParsing(TDABasicIntroParsingTests.line("<- ds.send y"));
		fsParser.tryParsing(line);
		nested.tryParsing(TDABasicIntroParsingTests.line("x <- y"));
		nested.tryParsing(TDABasicIntroParsingTests.line("v <- 42"));
	}

	@Test
	public void cannotDirectlyNestScopeUnderMethodWithoutActions() {
		// extract this here since we want to check it's the one that errors, but we only supply it later ...
		final Tokenizable line = TDABasicIntroParsingTests.line("s = 'hello'");
		context.checking(new Expectations() {{
			allowing(builder).name(); will(returnValue(name));
			allowing(errors).hasErrors(); will(returnValue(false));
			oneOf(builder).addConstructor(with(any(ObjectCtor.class)));
			oneOf(errors).message(line, "expected <-");
		}});
		TDAObjectElementsParser oep = new TDAObjectElementsParser(errors, builder, topLevel);
		TDAMethodMessageParser nested = (TDAMethodMessageParser) oep.tryParsing(TDABasicIntroParsingTests.line("ctor testMe"));
		nested.tryParsing(line);
	}
}
