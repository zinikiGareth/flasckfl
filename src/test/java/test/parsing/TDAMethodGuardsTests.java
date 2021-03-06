package test.parsing;

import static org.junit.Assert.assertTrue;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.GuardedMessagesConsumer;
import org.flasck.flas.parser.IgnoreNestedParser;
import org.flasck.flas.parser.LastOneOnlyNestedParser;
import org.flasck.flas.parser.MethodMessagesConsumer;
import org.flasck.flas.parser.TDAMethodGuardParser;
import org.flasck.flas.parser.TDAMethodMessageParser;
import org.flasck.flas.parser.TDAParsing;
import org.flasck.flas.tokenizers.Tokenizable;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import flas.matchers.ExprMatcher;
import flas.matchers.GuardedMessagesMatcher;
import flas.matchers.SendMessageMatcher;

public class TDAMethodGuardsTests {
	interface GM extends MethodMessagesConsumer, GuardedMessagesConsumer {}
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	private ErrorReporter errorsMock = context.mock(ErrorReporter.class);
	private LocalErrorTracker tracker = new LocalErrorTracker(errorsMock);
	private GM builder = context.mock(GM.class);
	private LastOneOnlyNestedParser nestedFunctionScope = context.mock(LastOneOnlyNestedParser.class);
	private InputPosition pos = new InputPosition("fred", 10, 0, null, "hello");

	@Before
	public void setup() {
		context.checking(new Expectations() {{
			allowing(nestedFunctionScope).anotherParent();
		}});
	}
	
	// When we first hit a method, we don't know if we are going to see guards or messages
	// Check we can see a messaqe
	@Test
	public void weDontNeedToUseGuards() {
		context.checking(new Expectations() {{
			oneOf(builder).sendMessage(with(SendMessageMatcher.of(ExprMatcher.member(ExprMatcher.unresolved("data"), ExprMatcher.unresolved("fetchRoot"))).location("fred", 1, 0, 2)));
			oneOf(builder).done();
		}});
		TDAMethodGuardParser parser = new TDAMethodGuardParser(tracker, builder, nestedFunctionScope);
		TDAParsing nested = parser.tryParsing(TDABasicIntroParsingTests.line("<- data.fetchRoot"));
		assertTrue(nested instanceof LastOneOnlyNestedParser);
		parser.scopeComplete(pos);
	}

	@Test
	public void seeingAGuardGeneratesAGuardAndReturnsAMessageParser() {
		context.checking(new Expectations() {{
			oneOf(builder).guard(with(GuardedMessagesMatcher.of(ExprMatcher.apply(ExprMatcher.typeref("True")))));
			oneOf(builder).done();
		}});
		TDAMethodGuardParser parser = new TDAMethodGuardParser(tracker, builder, nestedFunctionScope);
		TDAParsing nested = parser.tryParsing(TDABasicIntroParsingTests.line("| True"));
		assertTrue(nested instanceof TDAMethodMessageParser);
		parser.scopeComplete(pos);
	}

	@Test
	public void theFirstGuardCommitsUsToGuards() {
		context.checking(new Expectations() {{
			oneOf(builder).guard(with(GuardedMessagesMatcher.of(ExprMatcher.apply(ExprMatcher.typeref("True")))));
		}});
		TDAMethodGuardParser parser = new TDAMethodGuardParser(tracker, builder, nestedFunctionScope);
		TDAParsing nested = parser.tryParsing(TDABasicIntroParsingTests.line("| True"));
		assertTrue(nested instanceof TDAMethodMessageParser);
		context.assertIsSatisfied();
		Tokenizable toks = TDABasicIntroParsingTests.line("<- data.fetchRoot");
		context.checking(new Expectations() {{
			oneOf(errorsMock).message(with(any(InputPosition.class)), with("guard expected"));
			oneOf(builder).done();
		}});
		nested = parser.tryParsing(toks);
		assertTrue(nested instanceof IgnoreNestedParser);
		parser.scopeComplete(pos);
	}

	@Test
	public void theFirstMessageCommitsUsToMessages() {
		context.checking(new Expectations() {{
			oneOf(builder).sendMessage(with(SendMessageMatcher.of(ExprMatcher.member(ExprMatcher.unresolved("data"), ExprMatcher.unresolved("fetchRoot"))).location("fred", 1, 0, 2)));
		}});
		TDAMethodGuardParser parser = new TDAMethodGuardParser(tracker, builder, nestedFunctionScope);
		TDAParsing nested = parser.tryParsing(TDABasicIntroParsingTests.line("<- data.fetchRoot"));
		assertTrue(nested instanceof LastOneOnlyNestedParser);
		context.assertIsSatisfied();
		Tokenizable toks = TDABasicIntroParsingTests.line("| True");
		context.checking(new Expectations() {{
			oneOf(errorsMock).message(with(toks), with("expected assign or send message"));
			oneOf(builder).done();
		}});
		nested = parser.tryParsing(toks);
		assertTrue(nested instanceof IgnoreNestedParser);
		parser.scopeComplete(pos);
	}

	@Test
	public void theFinalGuardMayBeADefault() {
		context.checking(new Expectations() {{
			oneOf(builder).guard(with(GuardedMessagesMatcher.of(ExprMatcher.apply(ExprMatcher.typeref("True")))));
		}});
		TDAMethodGuardParser parser = new TDAMethodGuardParser(tracker, builder, nestedFunctionScope);
		TDAParsing nested = parser.tryParsing(TDABasicIntroParsingTests.line("| True"));
		assertTrue(nested instanceof TDAMethodMessageParser);
		context.assertIsSatisfied();
		Tokenizable toks = TDABasicIntroParsingTests.line("|");
		context.checking(new Expectations() {{
			oneOf(builder).guard(with(GuardedMessagesMatcher.of(null)));
			oneOf(builder).done();
		}});
		nested = parser.tryParsing(toks);
		assertTrue(nested instanceof TDAMethodMessageParser);
		parser.scopeComplete(pos);
	}

	@Test
	public void theFirstGuardMayNotBeADefault() {
		context.checking(new Expectations() {{
			oneOf(errorsMock).message(with(any(InputPosition.class)), with("first guard cannot be default"));
			oneOf(builder).done();
		}});
		TDAMethodGuardParser parser = new TDAMethodGuardParser(tracker, builder, nestedFunctionScope);
		TDAParsing nested = parser.tryParsing(TDABasicIntroParsingTests.line("|"));
		assertTrue(nested instanceof IgnoreNestedParser);
		parser.scopeComplete(pos);
	}

	@Test
	public void cannotHaveTwoDefaults() {
		context.checking(new Expectations() {{
			oneOf(builder).guard(with(GuardedMessagesMatcher.of(ExprMatcher.apply(ExprMatcher.typeref("True")))));
			oneOf(builder).guard(with(GuardedMessagesMatcher.of(null)));
			oneOf(errorsMock).message(with(any(InputPosition.class)), with("cannot provide two default guards"));
			oneOf(builder).done();
		}});
		TDAMethodGuardParser parser = new TDAMethodGuardParser(tracker, builder, nestedFunctionScope);
		TDAParsing nested = parser.tryParsing(TDABasicIntroParsingTests.line("| True"));
		nested = parser.tryParsing(TDABasicIntroParsingTests.line("|"));
		nested = parser.tryParsing(TDABasicIntroParsingTests.line("|"));
		assertTrue(nested instanceof IgnoreNestedParser);
		parser.scopeComplete(pos);
	}
}
