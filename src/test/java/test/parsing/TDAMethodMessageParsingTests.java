package test.parsing;

import static org.junit.Assert.assertTrue;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parser.IgnoreNestedParser;
import org.flasck.flas.parser.LastOneOnlyNestedParser;
import org.flasck.flas.parser.MethodMessagesConsumer;
import org.flasck.flas.parser.TDAMethodMessageParser;
import org.flasck.flas.parser.TDAParsing;
import org.flasck.flas.tokenizers.Tokenizable;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import flas.matchers.AssignMessageMatcher;
import flas.matchers.ExprMatcher;
import flas.matchers.SendMessageMatcher;

public class TDAMethodMessageParsingTests {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	private ErrorReporter errorsMock = context.mock(ErrorReporter.class);
	private LocalErrorTracker tracker = new LocalErrorTracker(errorsMock);
	private MethodMessagesConsumer builder = context.mock(MethodMessagesConsumer.class);
	private LastOneOnlyNestedParser nestedFunctionScope = context.mock(LastOneOnlyNestedParser.class);
	private InputPosition pos = new InputPosition("fred", 10, 0, null, "hello");

	@Before
	public void setup() {
		context.checking(new Expectations() {{
			allowing(nestedFunctionScope).anotherParent();
		}});
	}
	
	// In this corner, we have "SEND" messages, see Rule message-method-action
	@Test
	public void weCanInvokeSendOnAServiceWithoutAnyArguments() {
		context.checking(new Expectations() {{
			oneOf(builder).sendMessage(with(SendMessageMatcher.of(ExprMatcher.member(ExprMatcher.unresolved("data"), ExprMatcher.unresolved("fetchRoot"))).location("fred", 1, 0, 2)));
			oneOf(builder).done();
		}});
		TDAMethodMessageParser parser = new TDAMethodMessageParser(tracker, builder, nestedFunctionScope);
		TDAParsing nested = parser.tryParsing(TDABasicIntroParsingTests.line("<- data.fetchRoot"));
		// I'm not sure if this is quite right, because of the weird thing about the final method being able to have an indented block for everybody
		// That needs separate testing elsewhere
		assertTrue(nested instanceof LastOneOnlyNestedParser);
		parser.scopeComplete(pos);
	}

	@Test
	public void weCanInvokeSendOnAServiceWithOneArgument() {
		context.checking(new Expectations() {{
			oneOf(builder).sendMessage(with(SendMessageMatcher.of(ExprMatcher.apply(ExprMatcher.member(ExprMatcher.unresolved("data"), ExprMatcher.unresolved("get")), ExprMatcher.string("hello"))).location("fred", 1, 0, 2)));
		}});
		TDAMethodMessageParser parser = new TDAMethodMessageParser(tracker, builder, nestedFunctionScope);
		TDAParsing nested = parser.tryParsing(TDABasicIntroParsingTests.line("<- data.get 'hello'"));
		assertTrue(nested instanceof LastOneOnlyNestedParser);
	}

	@Test
	public void aSendCanHaveAHandler() {
		context.checking(new Expectations() {{
			oneOf(builder).sendMessage(with(SendMessageMatcher.of(ExprMatcher.apply(ExprMatcher.operator("->"), ExprMatcher.apply(ExprMatcher.member(ExprMatcher.unresolved("data"), ExprMatcher.unresolved("get")), ExprMatcher.string("hello")), ExprMatcher.unresolved("hdlr")))));
		}});
		TDAMethodMessageParser parser = new TDAMethodMessageParser(tracker, builder, nestedFunctionScope);
		TDAParsing nested = parser.tryParsing(TDABasicIntroParsingTests.line("<- data.get 'hello' -> hdlr"));
		assertTrue(nested instanceof LastOneOnlyNestedParser);
	}

	// And in this corner, we have "ASSIGN" messages, see Rule assign-method-action
	@Test
	public void weCanAssignToAStateMemberByName() {
		context.checking(new Expectations() {{
			oneOf(builder).assignMessage(with(AssignMessageMatcher.to("x").with(ExprMatcher.number(42)).location("fred", 1, 2, 4)));
		}});
		TDAMethodMessageParser parser = new TDAMethodMessageParser(tracker, builder, nestedFunctionScope);
		TDAParsing nested = parser.tryParsing(TDABasicIntroParsingTests.line("x <- 42"));
		assertTrue(nested instanceof LastOneOnlyNestedParser);
	}

	@Test
	public void weCanAssignToANestedMemberByPath() {
		context.checking(new Expectations() {{
			oneOf(builder).assignMessage(with(AssignMessageMatcher.to("x", "y").with(ExprMatcher.number(42)).location("fred", 1, 4, 6)));
		}});
		TDAMethodMessageParser parser = new TDAMethodMessageParser(tracker, builder, nestedFunctionScope);
		TDAParsing nested = parser.tryParsing(TDABasicIntroParsingTests.line("x.y <- 42"));
		assertTrue(nested instanceof LastOneOnlyNestedParser);
	}

	// TODO: What about things like "Debug"?
	// <- Debug "x"
	// I think anything that creates an "Action" class is OK, thus parser must accept anything that is an expression
	
	// and then we have a bunch of error cases
	@Test
	public void cantHaveJustArrowByItself() {
		context.checking(new Expectations() {{
			oneOf(errorsMock).message(with(any(Tokenizable.class)), with("no expression to send"));
		}});
		TDAMethodMessageParser parser = new TDAMethodMessageParser(tracker, builder, nestedFunctionScope);
		TDAParsing nested = parser.tryParsing(TDABasicIntroParsingTests.line("<-"));
		assertTrue(nested instanceof IgnoreNestedParser);
	}

	@Test
	public void cantAssignWithJustAnArrow() {
		context.checking(new Expectations() {{
			oneOf(errorsMock).message(with(any(Tokenizable.class)), with("no expression to send"));
		}});
		TDAMethodMessageParser parser = new TDAMethodMessageParser(tracker, builder, nestedFunctionScope);
		TDAParsing nested = parser.tryParsing(TDABasicIntroParsingTests.line("x <-"));
		assertTrue(nested instanceof IgnoreNestedParser);
	}

	@Test
	public void cantAssignASlotWithoutAnArrow() {
		context.checking(new Expectations() {{
			oneOf(errorsMock).message(with(any(InputPosition.class)), with("expected <-"));
		}});
		TDAMethodMessageParser parser = new TDAMethodMessageParser(tracker, builder, nestedFunctionScope);
		TDAParsing nested = parser.tryParsing(TDABasicIntroParsingTests.line("a.x 42"));
		assertTrue(nested instanceof IgnoreNestedParser);
	}

	@Test
	public void cantNestSlotsWithoutDot() {
		context.checking(new Expectations() {{
			oneOf(errorsMock).message(with(any(InputPosition.class)), with("expected <-"));
		}});
		TDAMethodMessageParser parser = new TDAMethodMessageParser(tracker, builder, nestedFunctionScope);
		TDAParsing nested = parser.tryParsing(TDABasicIntroParsingTests.line("a x <- 42"));
		assertTrue(nested instanceof IgnoreNestedParser);
	}

	@Test
	public void cantHaveTwoDots() {
		context.checking(new Expectations() {{
			oneOf(errorsMock).message(with(any(Tokenizable.class)), with("expected identifier"));
		}});
		TDAMethodMessageParser parser = new TDAMethodMessageParser(tracker, builder, nestedFunctionScope);
		TDAParsing nested = parser.tryParsing(TDABasicIntroParsingTests.line("a .. x <- 42"));
		assertTrue(nested instanceof IgnoreNestedParser);
	}

	@Test
	public void fieldCantBeConstant() {
		context.checking(new Expectations() {{
			oneOf(errorsMock).message(with(any(Tokenizable.class)), with("expected assign or send message"));
		}});
		TDAMethodMessageParser parser = new TDAMethodMessageParser(tracker, builder, nestedFunctionScope);
		TDAParsing nested = parser.tryParsing(TDABasicIntroParsingTests.line("'hello' <- 42"));
		assertTrue(nested instanceof IgnoreNestedParser);
	}

	@Test
	public void nestedFieldCantBeConstant() {
		context.checking(new Expectations() {{
			oneOf(errorsMock).message(with(any(Tokenizable.class)), with("expected identifier"));
		}});
		TDAMethodMessageParser parser = new TDAMethodMessageParser(tracker, builder, nestedFunctionScope);
		TDAParsing nested = parser.tryParsing(TDABasicIntroParsingTests.line("a . 15 <- 42"));
		assertTrue(nested instanceof IgnoreNestedParser);
	}
}
