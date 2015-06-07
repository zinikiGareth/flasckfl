package org.flasck.flas.parser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.PrintWriter;

import org.flasck.flas.errors.ErrorResult;
import org.flasck.flas.parsedForm.ApplyExpr;
import org.flasck.flas.parsedForm.EventHandler;
import org.flasck.flas.parsedForm.TemplateAttributeVar;
import org.flasck.flas.parsedForm.TemplateExplicitAttr;
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
		assertEquals("cannot have other content on line with . or +", er.errors.get(0).msg);
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
		assertFormat(TemplateToken.IDENTIFIER, "format", tl, 0);
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
		assertFormat(TemplateToken.IDENTIFIER, "format", tl, 0);
	}

	@Test
	public void testDivWithCustomTag() throws Exception {
		TemplateLine tl = parse(". #blockquote");
		assertEquals(1, tl.contents.size());
		assertToken(TemplateToken.DIV, ".", tl, 0);
		assertEquals("blockquote", tl.customTag);
		assertNull(tl.customTagVar);
		assertEquals(0, tl.formats.size());
	}

	@Test
	public void testDivWithCustomTagVar() throws Exception {
		TemplateLine tl = parse(". ## tag");
		assertEquals(1, tl.contents.size());
		assertToken(TemplateToken.DIV, ".", tl, 0);
		assertNull(tl.customTag);
		assertEquals("tag", tl.customTagVar);
		assertEquals(0, tl.formats.size());
	}

	@Test
	public void testDivWithCustomTagAndFormat() throws Exception {
		TemplateLine tl = parse(". #blockquote : 'quoted-text'");
		assertEquals(1, tl.contents.size());
		assertToken(TemplateToken.DIV, ".", tl, 0);
		assertEquals("blockquote", tl.customTag);
		assertNull(tl.customTagVar);
		assertEquals(1, tl.formats.size());
		assertFormat(TemplateToken.STRING, "quoted-text", tl, 0);
	}

	@Test
	public void testSimpleContentCannotHaveCustomTag() throws Exception {
		ErrorResult err = parseError("counter #blockquote");
		assertEquals(1, err.errors.size());
		assertEquals("can only use # by itself or with . or +", err.errors.get(0).msg);
	}

	@Test
	public void testCustomTagCannotHaveSimpleContent() throws Exception {
		ErrorResult err = parseError("#blockquote counter");
		assertEquals(1, err.errors.size());
		assertEquals("syntax error", err.errors.get(0).msg);
	}

	@Test
	public void testCustomTagDoesNotRequireDot() throws Exception {
		TemplateLine tl = parse("#blockquote");
		assertEquals(0, tl.contents.size());
		assertEquals("blockquote", tl.customTag);
		assertNull(tl.customTagVar);
		assertEquals(0, tl.formats.size());
	}

	@Test
	public void testCustomTagDoesNotRequireDotButCanStillHaveFormat() throws Exception {
		TemplateLine tl = parse("#blockquote : format");
		assertEquals(0, tl.contents.size());
		assertEquals("blockquote", tl.customTag);
		assertNull(tl.customTagVar);
		assertEquals(1, tl.formats.size());
		assertFormat(TemplateToken.IDENTIFIER, "format", tl, 0);
	}

	@Test
	public void testListWithCustomTag() throws Exception {
		TemplateLine tl = parse("+ list #ol");
		assertEquals(1, tl.contents.size());
		assertTrue(tl.contents.get(0) instanceof TemplateList);
		TemplateList lv = (TemplateList) tl.contents.get(0);
		assertEquals("list", lv.listVar);
		assertEquals("ol", tl.customTag);
		assertNull(tl.customTagVar);
		assertEquals(0, tl.formats.size());
	}

	@Test
	public void testTagCanHaveExplicitAttribute() throws Exception {
		TemplateLine tl = parse("#blockquote @id=famous");
		assertEquals(0, tl.contents.size());
		assertEquals("blockquote", tl.customTag);
		assertNull(tl.customTagVar);
		assertEquals(1, tl.attrs.size());
		assertTrue(tl.attrs.get(0) instanceof TemplateExplicitAttr);
		TemplateExplicitAttr attr = (TemplateExplicitAttr) tl.attrs.get(0);
		assertEquals("id", attr.attr);
		assertEquals(TemplateToken.IDENTIFIER, attr.type);
		assertEquals("famous", attr.value);
		assertEquals(0, tl.formats.size());
	}

	@Test
	public void testTagCanHaveExplicitStringAttribute() throws Exception {
		TemplateLine tl = parse("#blockquote @id='famous'");
		assertEquals(0, tl.contents.size());
		assertEquals("blockquote", tl.customTag);
		assertNull(tl.customTagVar);
		assertEquals(1, tl.attrs.size());
		assertTrue(tl.attrs.get(0) instanceof TemplateExplicitAttr);
		TemplateExplicitAttr attr = (TemplateExplicitAttr) tl.attrs.get(0);
		assertEquals("id", attr.attr);
		assertEquals(TemplateToken.STRING, attr.type);
		assertEquals("famous", attr.value);
		assertEquals(0, tl.formats.size());
	}

	@Test
	public void testTagCanHaveVariableForAttribute() throws Exception {
		TemplateLine tl = parse("#blockquote @@id");
		assertEquals(0, tl.contents.size());
		assertEquals("blockquote", tl.customTag);
		assertNull(tl.customTagVar);
		assertEquals(1, tl.attrs.size());
		assertTrue(tl.attrs.get(0) instanceof TemplateAttributeVar);
		TemplateAttributeVar attr = (TemplateAttributeVar) tl.attrs.get(0);
		assertEquals("id", attr.var);
		assertEquals(0, tl.formats.size());
	}

	@Test
	public void testWeCanHaveAnExpressionInATemplate() throws Exception {
		TemplateLine tl = parse("(fnCall 'hello' 3 x)");
		assertEquals(1, tl.contents.size());
		System.out.println(tl.contents.get(0).getClass());
		assertTrue(tl.contents.get(0) instanceof ApplyExpr);
		ApplyExpr ae = (ApplyExpr) tl.contents.get(0);
		assertEquals("fnCall", ae.fn.toString());
		assertEquals(3, ae.args.size());
		assertEquals("\"hello\"", ae.args.get(0).toString());
		assertEquals("3", ae.args.get(1).toString());
		assertEquals("x", ae.args.get(2).toString());
	}

	@Test
	public void testWeCanDefineAClickHandler() throws Exception {
		EventHandler eh = parseHandler("click => handleClick 7 (u:x)");
		assertEquals("click", eh.text);
		assertNull(eh.var);
		assertNotNull(eh.expr);
		assertTrue(eh.expr instanceof ApplyExpr);
		ApplyExpr ae = (ApplyExpr) eh.expr;
		assertEquals("handleClick", ae.fn.toString());
		assertEquals(2, ae.args.size());
		assertEquals("7", ae.args.get(0).toString());
		assertTrue(ae.args.get(1) instanceof ApplyExpr);
		ae = (ApplyExpr) ae.args.get(1);
		assertEquals("Cons", ae.fn.toString());
		assertEquals(2, ae.args.size());
		assertEquals("u", ae.args.get(0).toString());
		assertEquals("x", ae.args.get(1).toString());
	}

	@Test
	public void testAHandlerCanTakeAnEvent() throws Exception {
		EventHandler eh = parseHandler("mousedown ev => handleClick 7 (u:x)");
		assertEquals("mousedown", eh.text);
		assertEquals("ev", eh.var);
		assertNotNull(eh.expr);
		assertTrue(eh.expr instanceof ApplyExpr);
		ApplyExpr ae = (ApplyExpr) eh.expr;
		assertEquals("handleClick", ae.fn.toString());
		assertEquals(2, ae.args.size());
		assertEquals("7", ae.args.get(0).toString());
		assertTrue(ae.args.get(1) instanceof ApplyExpr);
		ae = (ApplyExpr) ae.args.get(1);
		assertEquals("Cons", ae.fn.toString());
		assertEquals(2, ae.args.size());
		assertEquals("u", ae.args.get(0).toString());
		assertEquals("x", ae.args.get(1).toString());
	}

	protected TemplateLine parse(String input) throws Exception {
		Object o = doparse(input);
		assertNotNull(o);
		assertTrue(o instanceof TemplateLine);
		return (TemplateLine) o;
	}

	protected EventHandler parseHandler(String input) throws Exception {
		Object o = doparse(input);
		assertNotNull(o);
		assertTrue(o instanceof EventHandler);
		return (EventHandler) o;
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
