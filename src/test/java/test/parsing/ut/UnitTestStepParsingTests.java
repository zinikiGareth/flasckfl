package test.parsing.ut;

import static org.junit.Assert.assertTrue;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parser.IgnoreNestedParser;
import org.flasck.flas.parser.NoNestingParser;
import org.flasck.flas.parser.TDAParsing;
import org.flasck.flas.parser.TopLevelNamer;
import org.flasck.flas.parser.ut.TestStepParser;
import org.flasck.flas.parser.ut.UnitTestStepConsumer;
import org.flasck.flas.tokenizers.Tokenizable;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Rule;
import org.junit.Test;

import test.parsing.ExprMatcher;
import test.parsing.LocalErrorTracker;

public class UnitTestStepParsingTests {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	private ErrorReporter errors = context.mock(ErrorReporter.class);
	private LocalErrorTracker tracker = new LocalErrorTracker(errors);
	private TopLevelNamer namer = context.mock(TopLevelNamer.class);
	private UnitTestStepConsumer builder = context.mock(UnitTestStepConsumer.class);
	private InputPosition pos = new InputPosition("fred", 10, 0, "hello");

	@Test
	public void testWeCanHandleASimpleAssertStep() {
		context.checking(new Expectations() {{
			oneOf(builder).assertion(with(ExprMatcher.unresolved("x")), with(ExprMatcher.number(86)));
		}});
		TestStepParser utp = new TestStepParser(tracker, namer, builder);
		TDAParsing nested = utp.tryParsing(UnitTestTopLevelParsingTests.line("assert x"));
		assertTrue(nested instanceof SingleExpressionParser);
		TDAParsing nnp = nested.tryParsing(UnitTestTopLevelParsingTests.line("86"));
		assertTrue(nnp instanceof NoNestingParser);
		nnp.scopeComplete(pos);
		nested.scopeComplete(pos);
		utp.scopeComplete(pos);
	}
	
	@Test
	public void testThatThereIsAnExpressionOnTheLine() {
		final Tokenizable line = UnitTestTopLevelParsingTests.line("assert");
		context.checking(new Expectations() {{
			oneOf(errors).message(line, "assert requires expression to evaluate");
		}});
		TestStepParser utp = new TestStepParser(tracker, namer, builder);
		TDAParsing nested = utp.tryParsing(line);
		assertTrue(nested instanceof IgnoreNestedParser);
	}
	
	@Test
	public void testThatThereIsAMatchExpression() {
		final Tokenizable line = UnitTestTopLevelParsingTests.line("assert x");
		context.checking(new Expectations() {{
			oneOf(errors).message(pos, "assert requires exactly one match expression");
		}});
		TestStepParser utp = new TestStepParser(tracker, namer, builder);
		TDAParsing nested = utp.tryParsing(line);
		assertTrue(nested instanceof SingleExpressionParser);
		nested.scopeComplete(pos);
	}
	
	@Test
	public void testThatThereIsOnlyOneExpressionOnTheLine() {
		final Tokenizable line = UnitTestTopLevelParsingTests.line("assert 42)");
		context.checking(new Expectations() {{
			oneOf(errors).message(with(any(InputPosition.class)), with("invalid tokens after expression"));
		}});
		TestStepParser utp = new TestStepParser(tracker, namer, builder);
		TDAParsing nested = utp.tryParsing(line);
		assertTrue(nested instanceof IgnoreNestedParser);
	}
	
	@Test
	public void testWeCanOnlyHandleOneMatchingExpression() {
		context.checking(new Expectations() {{
			oneOf(builder).assertion(with(ExprMatcher.unresolved("x")), with(ExprMatcher.number(86)));
			oneOf(errors).message(pos, "assert requires exactly one match expression");
		}});
		TestStepParser utp = new TestStepParser(tracker, namer, builder);
		TDAParsing nested = utp.tryParsing(UnitTestTopLevelParsingTests.line("assert x"));
		assertTrue(nested instanceof SingleExpressionParser);
		nested.tryParsing(UnitTestTopLevelParsingTests.line("86"));
		nested.tryParsing(UnitTestTopLevelParsingTests.line("32"));
		nested.scopeComplete(pos);
		utp.scopeComplete(pos);
	}
}
