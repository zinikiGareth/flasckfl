package org.flasck.flas.parser;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.PrintWriter;

import org.flasck.flas.ErrorResult;
import org.flasck.flas.parsedForm.TemplateLine;
import org.flasck.flas.tokenizers.TemplateToken;
import org.flasck.flas.tokenizers.Tokenizable;
import org.junit.Test;

public class TemplateParsingTests {
	private final TemplateLineParser p = new TemplateLineParser();

	@Test
	public void testSimpleVar() {
		TemplateLine tl = parse("counter");
		assertEquals(1, tl.contents.size());
		assertEquals(0, tl.formats.size());
		assertToken(TemplateToken.IDENTIFIER, "counter", tl, 0);
	}

	@Test
	public void testSimpleWithFormat() {
		TemplateLine tl = parse("counter: format");
		assertEquals(1, tl.contents.size());
		assertToken(TemplateToken.IDENTIFIER, "counter", tl, 0);
		assertEquals(1, tl.formats.size());
		assertFormat("format", tl, 0);
	}

	@Test
	public void testLiteralText() {
		TemplateLine tl = parse("'counter is: '");
		assertEquals(1, tl.contents.size());
		assertToken(TemplateToken.STRING, "counter is: ", tl, 0);
		assertEquals(0, tl.formats.size());
	}

	@Test
	public void testMultipleTokens() {
		TemplateLine tl = parse("'counter is: ' counter");
		assertEquals(2, tl.contents.size());
		assertToken(TemplateToken.STRING, "counter is: ", tl, 0);
		assertToken(TemplateToken.IDENTIFIER, "counter", tl, 1);
		assertEquals(0, tl.formats.size());
	}

	@Test
	public void testDiv() {
		TemplateLine tl = parse(".");
		assertEquals(1, tl.contents.size());
		assertToken(TemplateToken.DIV, ".", tl, 0);
		assertEquals(0, tl.formats.size());
	}

	@Test
	public void testDivCantBePartOfLongerLine() throws IOException {
		ErrorResult er = parseError(". counter");
		assertEquals(1, er.errors.size());
		assertEquals("Cannot have other content on line with <div>", er.errors.get(0).msg);
		er.showTo(new PrintWriter(System.out));
	}

	protected TemplateLine parse(String string) {
		Object o = doparse(string);
		assertNotNull(o);
		assertTrue(o instanceof TemplateLine);
		return (TemplateLine) o;
	}

	protected ErrorResult parseError(String string) {
		Object o = doparse(string);
		assertNotNull(o);
		assertTrue(o instanceof ErrorResult);
		return (ErrorResult) o;
	}

	private Object doparse(String string) {
		return p.tryParsing(new Tokenizable(string));
	}

	private void assertToken(int type, String text, TemplateLine tl, int which) {
		TemplateToken x = (TemplateToken) tl.contents.get(which);
		assertEquals("type of token " + which + " was wrong", type, x.type);
		assertEquals("text of token " + which + " was wrong", text, x.text);
	}

	private void assertFormat(String text, TemplateLine tl, int which) {
		assertEquals("format " + which + " was wrong", text, tl.formats.get(which));
	}
}
