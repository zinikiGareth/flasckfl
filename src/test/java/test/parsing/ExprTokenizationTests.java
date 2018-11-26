package test.parsing;

import static org.junit.Assert.assertNull;

import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parser.ExprTermConsumer;
import org.flasck.flas.parser.TDAExprParser;
import org.flasck.flas.tokenizers.Tokenizable;
import org.jmock.Expectations;
import org.jmock.Sequence;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Rule;
import org.junit.Test;

import test.flas.testrunner.ExprMatcher;

public class ExprTokenizationTests {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	private ErrorReporter errors = context.mock(ErrorReporter.class);
	private ExprTermConsumer builder = context.mock(ExprTermConsumer.class);
	private final TDAExprParser parser = new TDAExprParser(errors, builder);
	private final Sequence order = context.sequence("order");

	@Test
	public void testEndOfLineTerminates() {
		context.checking(new Expectations() {{
			oneOf(builder).done(); inSequence(order);
		}});
		assertNull(parser.tryParsing(new Tokenizable("")));
	}

	@Test
	public void testNumberIsParsedAsANumber() {
		context.checking(new Expectations() {{
			oneOf(builder).term(with(ExprMatcher.number(42).location("test", 1, 0, 2))); inSequence(order);
			oneOf(builder).done(); inSequence(order);
		}});
		assertNull(parser.tryParsing(new Tokenizable("42")));
	}

	@Test
	public void testStringInSingleQuotesIsParsedAsALiteral() {
		context.checking(new Expectations() {{
			oneOf(builder).term(with(ExprMatcher.string("hello").location("test", 1, 0, 7))); inSequence(order);
			oneOf(builder).done(); inSequence(order);
		}});
		assertNull(parser.tryParsing(new Tokenizable("'hello'")));
	}

	@Test
	public void testStringInDoubleQuotesIsParsedAsALiteral() {
		context.checking(new Expectations() {{
			oneOf(builder).term(with(ExprMatcher.string("hello").location("test", 1, 0, 7))); inSequence(order);
			oneOf(builder).done(); inSequence(order);
		}});
		assertNull(parser.tryParsing(new Tokenizable("\"hello\"")));
	}

	@Test
	public void testPlusIsParsedAsASymbol() {
		context.checking(new Expectations() {{
			oneOf(builder).term(with(ExprMatcher.operator("+").location("test", 1, 0, 1))); inSequence(order);
			oneOf(builder).done(); inSequence(order);
		}});
		assertNull(parser.tryParsing(new Tokenizable("+")));
	}

	@Test
	public void testORBIsParsedAsAPunc() {
		context.checking(new Expectations() {{
			oneOf(builder).term(with(ExprMatcher.punc("(").location("test", 1, 0, 1))); inSequence(order);
			oneOf(builder).done(); inSequence(order);
		}});
		assertNull(parser.tryParsing(new Tokenizable("(")));
	}

	@Test
	public void testCommaIsParsedAsAPunc() {
		context.checking(new Expectations() {{
			oneOf(builder).term(with(ExprMatcher.punc(",").location("test", 1, 0, 1))); inSequence(order);
			oneOf(builder).done(); inSequence(order);
		}});
		assertNull(parser.tryParsing(new Tokenizable(",")));
	}

	@Test
	public void testDotIsParsedAsASymbol() {
		context.checking(new Expectations() {{
			oneOf(builder).term(with(ExprMatcher.operator(".").location("test", 1, 0, 1))); inSequence(order);
			oneOf(builder).done(); inSequence(order);
		}});
		assertNull(parser.tryParsing(new Tokenizable(".")));
	}

	@Test
	public void testVarIsParsedAsAnUnresolvedVar() {
		context.checking(new Expectations() {{
			oneOf(builder).term(with(ExprMatcher.unresolved("x").location("test", 1, 0, 1))); inSequence(order);
			oneOf(builder).done(); inSequence(order);
		}});
		assertNull(parser.tryParsing(new Tokenizable("x")));
	}

	@Test
	public void testNilBecomesAConstructorAsAValue() {
		context.checking(new Expectations() {{
			oneOf(builder).term(with(ExprMatcher.unresolved("Nil").location("test", 1, 0, 3))); inSequence(order);
			oneOf(builder).done(); inSequence(order);
		}});
		assertNull(parser.tryParsing(new Tokenizable("Nil")));
	}

	@Test
	public void testASequenceOfItems() {
		context.checking(new Expectations() {{
			oneOf(builder).term(with(ExprMatcher.number(42).location("test", 1, 0, 2))); inSequence(order);
			oneOf(builder).term(with(ExprMatcher.operator("+").location("test", 1, 3, 4))); inSequence(order);
			oneOf(builder).term(with(ExprMatcher.unresolved("f").location("test", 1, 5, 6))); inSequence(order);
			oneOf(builder).term(with(ExprMatcher.string("hello").location("test", 1, 7, 14))); inSequence(order);
			oneOf(builder).done(); inSequence(order);
		}});
		assertNull(parser.tryParsing(new Tokenizable("42 + f 'hello'")));
	}

	@Test
	public void testASequenceOfItemsWithoutSpaces() {
		context.checking(new Expectations() {{
			oneOf(builder).term(with(ExprMatcher.number(42).location("test", 1, 0, 2))); inSequence(order);
			oneOf(builder).term(with(ExprMatcher.operator("+").location("test", 1, 2, 3))); inSequence(order);
			oneOf(builder).term(with(ExprMatcher.unresolved("f").location("test", 1, 3, 4))); inSequence(order);
			oneOf(builder).term(with(ExprMatcher.string("hello").location("test", 1, 4, 11))); inSequence(order);
			oneOf(builder).done(); inSequence(order);
		}});
		assertNull(parser.tryParsing(new Tokenizable("42+f'hello'")));
	}
}
