package org.flasck.flas.parser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.PrintWriter;

import org.flasck.flas.ErrorResult;
import org.flasck.flas.parsedForm.TemplateLine;
import org.flasck.flas.parsedForm.TemplateList;
import org.flasck.flas.tokenizers.TemplateToken;
import org.flasck.flas.tokenizers.Tokenizable;
import org.junit.Test;

public class TemplateParsingTests {
	private final TemplateLineParser p = new TemplateLineParser();

	@Test
	public void testSimpleVar() throws Exception {
		TemplateLine tl = parse("counter");
		assertEquals(1, tl.contents.size());
		assertEquals(0, tl.formats.size());
		assertToken(TemplateToken.IDENTIFIER, "counter", tl, 0);
	}

	@Test
	public void testSimpleWithFormat() throws Exception {
		TemplateLine tl = parse("counter: 'format'");
		assertEquals(1, tl.contents.size());
		assertToken(TemplateToken.IDENTIFIER, "counter", tl, 0);
		assertEquals(1, tl.formats.size());
		assertFormat(TemplateToken.STRING, "format", tl, 0);
	}

	@Test
	public void testSimpleWithMultipleFormats() throws Exception {
		TemplateLine tl = parse("counter: format 'style' settings");
		assertEquals(1, tl.contents.size());
		assertToken(TemplateToken.IDENTIFIER, "counter", tl, 0);
		assertEquals(3, tl.formats.size());
		assertFormat(TemplateToken.IDENTIFIER, "format", tl, 0);
		assertFormat(TemplateToken.STRING, "style", tl, 1);
		assertFormat(TemplateToken.IDENTIFIER, "settings", tl, 2);
	}

	@Test
	public void testLiteralText() throws Exception {
		TemplateLine tl = parse("'counter is: '");
		assertEquals(1, tl.contents.size());
		assertToken(TemplateToken.STRING, "counter is: ", tl, 0);
		assertEquals(0, tl.formats.size());
	}

	@Test
	public void testMultipleTokens() throws Exception {
		TemplateLine tl = parse("'counter is: ' counter");
		assertEquals(2, tl.contents.size());
		assertToken(TemplateToken.STRING, "counter is: ", tl, 0);
		assertToken(TemplateToken.IDENTIFIER, "counter", tl, 1);
		assertEquals(0, tl.formats.size());
	}

	@Test
	public void testDiv() throws Exception {
		TemplateLine tl = parse(".");
		assertEquals(1, tl.contents.size());
		assertToken(TemplateToken.DIV, ".", tl, 0);
		assertEquals(0, tl.formats.size());
	}

	@Test
	public void testDivCantBePartOfLongerLine() throws Exception {
		ErrorResult er = parseError(". counter");
		assertEquals(1, er.errors.size());
		assertEquals("Cannot have other content on line with . or +", er.errors.get(0).msg);
	}

	@Test
	public void testDivWithFormat() throws Exception {
		TemplateLine tl = parse(". : 'format'");
		assertEquals(1, tl.contents.size());
		assertToken(TemplateToken.DIV, ".", tl, 0);
		assertEquals(1, tl.formats.size());
		assertFormat(TemplateToken.STRING, "format", tl, 0);
	}

	@Test
	public void testSimpleList() throws Exception {
		TemplateLine tl = parse("+ list");
		assertEquals(1, tl.contents.size());
		assertTrue(tl.contents.get(0) instanceof TemplateList);
		TemplateList lv = (TemplateList) tl.contents.get(0);
		assertEquals("list", lv.listVar);
		assertNull(lv.iterVar);
		assertEquals(0, tl.formats.size());
	}

	@Test
	public void testSimpleListWithIterator() throws Exception {
		TemplateLine tl = parse("+ list iter");
		assertEquals(1, tl.contents.size());
		assertTrue(tl.contents.get(0) instanceof TemplateList);
		TemplateList lv = (TemplateList) tl.contents.get(0);
		assertEquals("list", lv.listVar);
		assertEquals("iter", lv.iterVar);
		assertEquals(0, tl.formats.size());
	}

	@Test
	public void testSimpleListWithFormat() throws Exception {
		TemplateLine tl = parse("+ list : format");
		assertEquals(1, tl.contents.size());
		assertTrue(tl.contents.get(0) instanceof TemplateList);
		TemplateList lv = (TemplateList) tl.contents.get(0);
		assertEquals("list", lv.listVar);
		assertNull(lv.iterVar);
		assertEquals(1, tl.formats.size());
	}

	@Test
	public void testSimpleListWithIteratorAndFormat() throws Exception {
		TemplateLine tl = parse("+ list iter : format");
		assertEquals(1, tl.contents.size());
		assertTrue(tl.contents.get(0) instanceof TemplateList);
		TemplateList lv = (TemplateList) tl.contents.get(0);
		assertEquals("list", lv.listVar);
		assertEquals("iter", lv.iterVar);
		assertEquals(1, tl.formats.size());
	}

	protected TemplateLine parse(String string) throws Exception {
		Object o = doparse(string);
		assertNotNull(o);
		assertTrue(o instanceof TemplateLine);
		return (TemplateLine) o;
	}

	protected ErrorResult parseError(String string) throws Exception {
		Object o = doparse(string);
		assertNotNull(o);
		assertTrue(o instanceof ErrorResult);
		return (ErrorResult) o;
	}

	private Object doparse(String string) throws Exception {
		Object ret = p.tryParsing(new Tokenizable(string));
		if (ret instanceof ErrorResult)
			((ErrorResult)ret).showTo(new PrintWriter(System.out));
		return ret;

	}

	private void assertToken(int type, String text, TemplateLine tl, int which) {
		TemplateToken x = (TemplateToken) tl.contents.get(which);
		assertEquals("type of token " + which + " was wrong", type, x.type);
		assertEquals("text of token " + which + " was wrong", text, x.text);
	}

	private void assertFormat(int type, String text, TemplateLine tl, int which) {
		TemplateToken x = tl.formats.get(which);
		assertEquals("type of format " + which + " was wrong", type, x.type);
		assertEquals("text of format " + which + " was wrong", text, x.text);
	}
}
