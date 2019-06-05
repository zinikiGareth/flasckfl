package test.parsing;

import static org.junit.Assert.assertTrue;

import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parser.IgnoreNestedParser;
import org.flasck.flas.parser.MethodMessagesConsumer;
import org.flasck.flas.parser.NoNestingParser;
import org.flasck.flas.parser.TDAMethodMessageParser;
import org.flasck.flas.parser.TDAParsing;
import org.flasck.flas.parser.TDAStructFieldParser;
import org.flasck.flas.tokenizers.Tokenizable;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

public class TDAMethodMessageParsingTests {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	private ErrorReporter errorsMock = context.mock(ErrorReporter.class);
	private MethodMessagesConsumer builder = context.mock(MethodMessagesConsumer.class);
	
	// In this corner, we have "SEND" messages, see Rule message-method-action
	@Test
	public void weCanInvokeSendOnAServiceWithoutAnyArguments() {
		context.checking(new Expectations() {{
			oneOf(builder).sendMessage(with(SendMessageMatcher.of(ExprMatcher.apply(ExprMatcher.operator("."), ExprMatcher.unresolved("data"), ExprMatcher.unresolved("fetchRoot"))).location("fred", 1, 0, 2)));
		}});
		TDAMethodMessageParser parser = new TDAMethodMessageParser(errorsMock, builder);
		TDAParsing nested = parser.tryParsing(TDABasicIntroParsingTests.line("<- data.fetchRoot"));
		// I'm not sure if this is quite right, because of the weird thing about the final method being able to have an indented block for everybody
		// That needs separate testing elsewhere
		assertTrue(nested instanceof NoNestingParser);
	}

	@Test
	public void weCanInvokeSendOnAServiceWithOneArgument() {
		context.checking(new Expectations() {{
			oneOf(builder).sendMessage(with(SendMessageMatcher.of(ExprMatcher.apply(ExprMatcher.apply(ExprMatcher.operator("."), ExprMatcher.unresolved("data"), ExprMatcher.unresolved("get")), ExprMatcher.string("hello"))).location("fred", 1, 0, 2)));
		}});
		TDAMethodMessageParser parser = new TDAMethodMessageParser(errorsMock, builder);
		TDAParsing nested = parser.tryParsing(TDABasicIntroParsingTests.line("<- data.get 'hello'"));
		// I'm not sure if this is quite right, because of the weird thing about the final method being able to have an indented block for everybody
		// That needs separate testing elsewhere
		assertTrue(nested instanceof NoNestingParser);
	}

	// And in this corner, we have "ASSIGN" messages, see Rule assign-method-action
	@Test
	@Ignore
	public void objectsCanHaveAStateParser() {
		context.checking(new Expectations() {{
//			oneOf(builder).defineState(with(any(StateDefinition.class)));
		}});
		TDAMethodMessageParser parser = new TDAMethodMessageParser(errorsMock, builder);
		TDAParsing nested = parser.tryParsing(TDABasicIntroParsingTests.line("state"));
		assertTrue(nested instanceof TDAStructFieldParser);
	}

	// TODO: What about things like "Debug"?
	// <- Debug "x"
	// I think anything that creates an "Action" class is OK, thus parser must accept anything that is an expression
	
	// and then we have a bunch of error cases
	@Test
	@Ignore
	public void junkIsNotAKeyword() {
		context.checking(new Expectations() {{
			oneOf(errorsMock).message(with(any(Tokenizable.class)), with("'junk' is not a valid object keyword"));
		}});
		TDAMethodMessageParser parser = new TDAMethodMessageParser(errorsMock, builder);
		TDAParsing nested = parser.tryParsing(TDABasicIntroParsingTests.line("junk"));
		assertTrue(nested instanceof IgnoreNestedParser);
	}

}
