package test.tokenizers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.tokenizers.StringToken;
import org.flasck.flas.tokenizers.Tokenizable;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Rule;
import org.junit.Test;

public class TestStringToken {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	ErrorReporter errors = context.mock(ErrorReporter.class);
	
	@Test
	public void testSimpleString() {
		String tok = StringToken.from(errors, new Tokenizable("'hello, world'"));
		assertEquals("hello, world", tok);
	}

	@Test
	public void testSimpleStringWithDQ() {
		String tok = StringToken.from(errors, new Tokenizable("\"hello, world\""));
		assertEquals("hello, world", tok);
	}

	@Test
	public void testPartialString() {
		Tokenizable line = new Tokenizable("'hello, world' is a string");
		String tok = StringToken.from(errors, line);
		assertEquals("hello, world", tok);
		assertEquals(' ', line.nextChar());
	}

	@Test
	public void testStringCanIncludeDQ() {
		Tokenizable line = new Tokenizable("'hello, \"world\"'");
		String tok = StringToken.from(errors, line);
		assertEquals("hello, \"world\"", tok);
	}

	@Test
	public void testDQStringCanIncludeSQ() {
		Tokenizable line = new Tokenizable("\"hello, Fred's world\"");
		String tok = StringToken.from(errors, line);
		assertEquals("hello, Fred's world", tok);
	}

	@Test
	public void testSQStringCanIncludeDoubleSQAsSQ() {
		Tokenizable line = new Tokenizable("'hello, Fred''s world'");
		String tok = StringToken.from(errors, line);
		assertEquals("hello, Fred's world", tok);
	}

	@Test
	public void testSQStringCanEndWithSQIfDouble() {
		Tokenizable line = new Tokenizable("'hello, Fred'''");
		String tok = StringToken.from(errors, line);
		assertEquals("hello, Fred'", tok);
	}

	@Test
	public void testNotQuoted() {
		String tok = StringToken.from(errors, new Tokenizable("not quoted"));
		assertNull(tok);
	}

	@Test
	public void testNoEndQuote() {
		context.checking(new Expectations() {{
			oneOf(errors).message(new InputPosition("test", 1, 0, null, ""), "unterminated string");
		}});
		String tok = StringToken.from(errors, new Tokenizable("'no end quote"));
		assertNull(tok);
	}

}
