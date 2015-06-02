package org.flasck.flas.parser;

import static org.junit.Assert.*;

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

	protected TemplateLine parse(String string) {
		Object o = doparse(string);
		assertNotNull(o);
		assertTrue(o instanceof TemplateLine);
		return (TemplateLine) o;
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
