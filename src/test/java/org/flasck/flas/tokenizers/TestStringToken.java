package org.flasck.flas.tokenizers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class TestStringToken {

	@Test
	public void testSimpleString() {
		String tok = StringToken.from(new Tokenizable("'hello, world'"));
		assertEquals("hello, world", tok);
	}

	@Test
	public void testSimpleStringWithDQ() {
		String tok = StringToken.from(new Tokenizable("\"hello, world\""));
		assertEquals("hello, world", tok);
	}

	@Test
	public void testPartialString() {
		Tokenizable line = new Tokenizable("'hello, world' is a string");
		String tok = StringToken.from(line);
		assertEquals("hello, world", tok);
		assertEquals(' ', line.nextChar());
	}

	@Test
	public void testStringCanIncludeDQ() {
		Tokenizable line = new Tokenizable("'hello, \"world\"'");
		String tok = StringToken.from(line);
		assertEquals("hello, \"world\"", tok);
	}

	@Test
	public void testDQStringCanIncludeSQ() {
		Tokenizable line = new Tokenizable("\"hello, Fred's world\"");
		String tok = StringToken.from(line);
		assertEquals("hello, Fred's world", tok);
	}

	@Test
	public void testSQStringCanIncludeDoubleSQAsSQ() {
		Tokenizable line = new Tokenizable("'hello, Fred''s world'");
		String tok = StringToken.from(line);
		assertEquals("hello, Fred's world", tok);
	}

	@Test
	public void testSQStringCanEndWithSQIfDouble() {
		Tokenizable line = new Tokenizable("'hello, Fred'''");
		String tok = StringToken.from(line);
		assertEquals("hello, Fred'", tok);
	}

	@Test
	public void testNotQuoted() {
		String tok = StringToken.from(new Tokenizable("not quoted"));
		assertNull(tok);
	}

	@Test
	public void testNoEndQuote() {
		String tok = StringToken.from(new Tokenizable("'no end quote"));
		assertNull(tok);
	}

}
