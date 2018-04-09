package org.flasck.flas.parser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.PrintWriter;
import java.util.List;

import org.flasck.flas.commonBase.ApplyExpr;
import org.flasck.flas.commonBase.NumericLiteral;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.commonBase.template.TemplateAttributeVar;
import org.flasck.flas.errors.ErrorResult;
import org.flasck.flas.errors.FLASError;
import org.flasck.flas.parsedForm.ContentExpr;
import org.flasck.flas.parsedForm.ContentString;
import org.flasck.flas.parsedForm.EventHandler;
import org.flasck.flas.parsedForm.TemplateDiv;
import org.flasck.flas.parsedForm.TemplateExplicitAttr;
import org.flasck.flas.parsedForm.TemplateFormat;
import org.flasck.flas.parsedForm.TemplateLine;
import org.flasck.flas.parsedForm.TemplateList;
import org.flasck.flas.parsedForm.TemplateReference;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.tokenizers.TemplateToken;
import org.flasck.flas.tokenizers.Tokenizable;
import org.junit.Test;

public class TemplateLineParsingTests {
	private final TemplateLineParser p = new TemplateLineParser();

	@Test
	public void testSimpleVar() throws Exception {
		List<TemplateLine> tls = parseContent("counter");
		assertEquals(1, tls.size());
		assertUVar("counter", tls, 0);
		assertFormats(0, tls, 0);
	}

	@Test
	public void testSimpleWithFormat() throws Exception {
		List<TemplateLine> tls = parseContent("counter: 'format'");
		assertEquals(1, tls.size());
		assertUVar("counter", tls, 0);
		assertFormats(1, tls, 0);
		assertFormat("format", tls, 0, 0);
	}

	@Test
	public void testSimpleWithMultipleFormats() throws Exception {
		List<TemplateLine> tls = parseContent("counter: format 'style' settings");
		assertEquals(1, tls.size());
		assertUVar("counter", tls, 0);
		assertFormats(3, tls, 0);
		assertFormatVar("format", tls, 0, 0);
		assertFormat("style", tls, 0, 1);
		assertFormatVar("settings", tls, 0, 2);
	}

	@Test
	public void testLiteralText() throws Exception {
		List<TemplateLine> tls = parseContent("'counter is: '");
		assertEquals(1, tls.size());
		assertString("counter is: ", tls, 0);
		assertFormats(0, tls, 0);
	}

	@Test
	public void testMultipleTokens() throws Exception {
		List<TemplateLine> tls = parseContent("'counter is: ' counter");
		assertEquals(2, tls.size());
		assertString("counter is: ", tls, 0);
		assertFormats(0, tls, 0);
		assertUVar("counter", tls, 1);
		assertFormats(0, tls, 1);
	}

	@Test
	public void testDiv() throws Exception {
		TemplateDiv tl = parseDiv(".");
		assertEquals(0, tl.attrs.size());
		assertNull(tl.customTag);
		assertNull(tl.customTagVar);
		assertEquals(0, tl.formats.size());
		assertEquals(0, tl.handlers.size());
		assertEquals(0, tl.nested.size());
	}

	@Test
	public void testDivCantBePartOfLongerLine() throws Exception {
		ErrorResult er = parseError(". counter");
		assertEquals(1, er.count());
		assertEquals("div or list must be only item on line", er.get(0).msg);
	}

	@Test
	public void testDivWithFormat() throws Exception {
		TemplateDiv tl = parseDiv(". : 'format'");
		assertEquals(0, tl.attrs.size());
		assertNull(tl.customTag);
		assertNull(tl.customTagVar);
		assertEquals(1, tl.formats.size());
		assertFormat("format", tl, 0);
		assertEquals(0, tl.handlers.size());
		assertEquals(0, tl.nested.size());
	}

	@Test
	public void testSimpleList() throws Exception {
		TemplateList tl = parseList("+ list");
		assertTrue(tl.listExpr instanceof UnresolvedVar);
		assertEquals("list", ((UnresolvedVar)tl.listExpr).var);
		assertNull(tl.iterVar);
		assertNull(tl.customTag);
		assertNull(tl.customTagVar);
		assertEquals(0, tl.formats.size());
	}

	@Test
	public void testSimpleListWithIterator() throws Exception {
		TemplateList tl = parseList("+ list iter");
		assertTrue(tl.listExpr instanceof UnresolvedVar);
		assertEquals("list", ((UnresolvedVar)tl.listExpr).var);
		assertEquals("iter", tl.iterVar);
		assertNull(tl.customTag);
		assertNull(tl.customTagVar);
		assertEquals(0, tl.formats.size());
	}

	@Test
	public void testSimpleListWithFormat() throws Exception {
		TemplateList tl = parseList("+ list : 'format'");
		assertTrue(tl.listExpr instanceof UnresolvedVar);
		assertEquals("list", ((UnresolvedVar)tl.listExpr).var);
		assertNull(tl.iterVar);
		assertNull(tl.customTag);
		assertNull(tl.customTagVar);
		assertEquals(1, tl.formats.size());
		assertFormat("format", tl, 0);
	}

	@Test
	public void testSimpleListWithIteratorAndFormat() throws Exception {
		TemplateList tl = parseList("+ list iter : 'format'");
		assertTrue(tl.listExpr instanceof UnresolvedVar);
		assertEquals("list", ((UnresolvedVar)tl.listExpr).var);
		assertEquals("iter", tl.iterVar);
		assertNull(tl.customTag);
		assertNull(tl.customTagVar);
		assertEquals(1, tl.formats.size());
		assertFormat("format", tl, 0);
	}

	@Test
	public void testListWIthExpressionIterator() throws Exception {
		TemplateList tl = parseList("+ (x.list) iter : 'format'");
		assertTrue(tl.listExpr instanceof ApplyExpr);
		assertEquals("(. x list)", tl.listExpr.toString());
		assertEquals("iter", tl.iterVar);
		assertNull(tl.customTag);
		assertNull(tl.customTagVar);
		assertEquals(1, tl.formats.size());
		assertFormat("format", tl, 0);
	}

	@Test
	public void testDivWithCustomTag() throws Exception {
		TemplateDiv tl = parseDiv(". #blockquote");
		assertEquals(0, tl.attrs.size());
		assertEquals("blockquote", tl.customTag);
		assertNull(tl.customTagVar);
		assertEquals(0, tl.formats.size());
		assertEquals(0, tl.handlers.size());
		assertEquals(0, tl.nested.size());
	}

	@Test
	public void testDivWithCustomTagVar() throws Exception {
		TemplateDiv tl = parseDiv(". ## tag");
		assertEquals(0, tl.attrs.size());
		assertNull(tl.customTag);
		assertEquals("tag", tl.customTagVar);
		assertEquals(0, tl.formats.size());
		assertEquals(0, tl.handlers.size());
		assertEquals(0, tl.nested.size());
	}

	@Test
	public void testDivWithCustomTagAndFormat() throws Exception {
		TemplateDiv tl = parseDiv(". #blockquote : 'quoted-text'");
		assertEquals(0, tl.attrs.size());
		assertEquals("blockquote", tl.customTag);
		assertNull(tl.customTagVar);
		assertEquals(1, tl.formats.size());
		assertFormat("quoted-text", tl, 0);
		assertEquals(0, tl.handlers.size());
		assertEquals(0, tl.nested.size());
	}

	@Test
	public void testSimpleContentCannotHaveCustomTag() throws Exception {
		ErrorResult err = parseError("counter #blockquote");
		assertEquals(1, err.count());
		assertEquals("can only use # by itself or with . or +", err.get(0).msg);
	}

	@Test
	public void testCustomTagCannotHaveSimpleContent() throws Exception {
		ErrorResult err = parseError("#blockquote counter");
		assertEquals(1, err.count());
		assertEquals("syntax error", err.get(0).msg);
	}

	@Test
	public void testCustomTagDoesNotRequireDot() throws Exception {
		TemplateDiv tl = parseDiv("#blockquote");
		assertEquals(0, tl.attrs.size());
		assertEquals("blockquote", tl.customTag);
		assertNull(tl.customTagVar);
		assertEquals(0, tl.formats.size());
		assertEquals(0, tl.handlers.size());
		assertEquals(0, tl.nested.size());
	}

	@Test
	public void testCustomTagDoesNotRequireDotButCanStillHaveFormat() throws Exception {
		TemplateDiv tl = parseDiv("#blockquote : 'format'");
		assertEquals(0, tl.attrs.size());
		assertEquals("blockquote", tl.customTag);
		assertNull(tl.customTagVar);
		assertEquals(1, tl.formats.size());
		assertFormat("format", tl, 0);
		assertEquals(0, tl.handlers.size());
		assertEquals(0, tl.nested.size());
	}

	@Test
	public void testListWithCustomTag() throws Exception {
		TemplateList tl = parseList("+ list #ol");
		assertTrue(tl.listExpr instanceof UnresolvedVar);
		assertEquals("list", ((UnresolvedVar)tl.listExpr).var);
		assertNull(tl.iterVar);
		assertEquals("ol", tl.customTag);
		assertNull(tl.customTagVar);
		assertEquals(0, tl.formats.size());
	}

	@Test
	public void testTagCanHaveExplicitStringAttribute() throws Exception {
		TemplateDiv tl = parseDiv("#blockquote @id='famous'");
		assertEquals(1, tl.attrs.size());
		TemplateExplicitAttr tea = (TemplateExplicitAttr) tl.attrs.get(0);
		assertEquals(TemplateToken.STRING, tea.type);
		assertEquals("id", tea.attr);
		assertEquals("famous", tea.value);
		assertEquals("blockquote", tl.customTag);
		assertNull(tl.customTagVar);
		assertEquals(0, tl.formats.size());
		assertEquals(0, tl.handlers.size());
		assertEquals(0, tl.nested.size());
	}

	@Test
	public void testAnExplicitStringAttributeCanStandByItself() throws Exception {
		TemplateDiv tl = parseDiv("@id='famous'");
		assertEquals(1, tl.attrs.size());
		TemplateExplicitAttr tea = (TemplateExplicitAttr) tl.attrs.get(0);
		assertEquals(TemplateToken.STRING, tea.type);
		assertEquals("id", tea.attr);
		assertEquals("famous", tea.value);
		assertNull(tl.customTag);
		assertNull(tl.customTagVar);
		assertEquals(0, tl.formats.size());
		assertEquals(0, tl.handlers.size());
		assertEquals(0, tl.nested.size());
	}

	@Test
	public void testTagCanHaveExplicitVariableAttribute() throws Exception {
		TemplateDiv tl = parseDiv("#blockquote @id=famous");
		assertEquals("blockquote", tl.customTag);
		assertEquals(1, tl.attrs.size());
		TemplateExplicitAttr tea = (TemplateExplicitAttr) tl.attrs.get(0);
		assertEquals(TemplateToken.IDENTIFIER, tea.type);
		assertEquals("id", tea.attr);
		assertTrue("was " + tea.value.getClass() + "; not UnresolvedVar", tea.value instanceof UnresolvedVar);
		assertEquals("famous", ((UnresolvedVar)tea.value).var);
		assertNull(tl.customTagVar);
		assertEquals(0, tl.formats.size());
		assertEquals(0, tl.handlers.size());
		assertEquals(0, tl.nested.size());
	}

	@Test
	public void testTagCanHaveParenedExprAsExplicitAttribute() throws Exception {
		TemplateDiv tl = parseDiv("#blockquote @id=(famous 36)");
		assertEquals("blockquote", tl.customTag);
		assertEquals(1, tl.attrs.size());
		TemplateExplicitAttr tea = (TemplateExplicitAttr) tl.attrs.get(0);
		assertEquals(TemplateToken.IDENTIFIER, tea.type);
		assertEquals("id", tea.attr);
		assertTrue("was " + tea.value.getClass() + "; not ApplyExpr", tea.value instanceof ApplyExpr);
		ApplyExpr ae = (ApplyExpr) tea.value;
		assertTrue(ae.fn instanceof UnresolvedVar);
		assertEquals("famous", ((UnresolvedVar)ae.fn).var);
		assertEquals("36", ae.args.get(0).toString());
		assertNull(tl.customTagVar);
		assertEquals(0, tl.formats.size());
		assertEquals(0, tl.handlers.size());
		assertEquals(0, tl.nested.size());
	}

	@Test
	public void testTagCanHaveVariableForAttribute() throws Exception {
		TemplateDiv tl = parseDiv("#blockquote @@id");
		assertEquals(1, tl.attrs.size());
		TemplateAttributeVar tav = (TemplateAttributeVar) tl.attrs.get(0);
		assertEquals("id", tav.var);
		assertEquals("blockquote", tl.customTag);
		assertNull(tl.customTagVar);
		assertEquals(0, tl.formats.size());
		assertEquals(0, tl.handlers.size());
		assertEquals(0, tl.nested.size());
	}

	@Test
	public void testDivCanReferenceAWebzipCard() throws Exception {
		TemplateDiv tl = parseDiv("%webzip");
		assertNotNull(tl);
		assertEquals("webzip", tl.webzip);
	}

	@Test
	public void testWeCanHaveAnExpressionInATemplate() throws Exception {
		List<TemplateLine> tls = parseContent("(fnCall 'hello' 3 x)");
		assertEquals(1, tls.size());
		assertTrue(tls.get(0) instanceof ContentExpr);
		ContentExpr tl = (ContentExpr) tls.get(0);
		assertTrue(tl.expr instanceof ApplyExpr);
		ApplyExpr ae = (ApplyExpr) tl.expr;
		assertEquals("fnCall", ae.fn.toString());
		assertEquals(3, ae.args.size());
		assertEquals("\"hello\"", ae.args.get(0).toString());
		assertEquals("3", ae.args.get(1).toString());
		assertEquals("x", ae.args.get(2).toString());
	}

	@Test
	public void testAsASpecialCaseWeCanHaveADottedExpressionInATemplateWithoutParens() throws Exception {
		List<TemplateLine> tls = parseContent("x.b");
		assertEquals(1, tls.size());
		assertTrue(tls.get(0) instanceof ContentExpr);
		ContentExpr tl = (ContentExpr) tls.get(0);
		assertTrue("was " + tl.expr.getClass() + " not ApplyExpr", tl.expr instanceof ApplyExpr);
		ApplyExpr ae = (ApplyExpr) tl.expr;
		assertEquals(".", ae.fn.toString());
		assertEquals(2, ae.args.size());
		assertEquals("x", ae.args.get(0).toString());
		assertEquals("b", ae.args.get(1).toString());
	}

	@Test
	public void testADottedExpressionInATemplateWithoutParensMustHaveAField() throws Exception {
		ErrorResult er = parseError("x.");
		assertEquals(1, er.count());
		FLASError err = er.get(0);
		assertEquals("missing field", err.msg);
	}

	@Test
	public void testADottedExpressionInATemplateWithoutParensCannotHaveTwoDots() throws Exception {
		ErrorResult er = parseError("x..b");
		assertEquals(1, er.count());
		FLASError err = er.get(0);
		assertEquals("syntax error", err.msg);
	}

	@Test
	public void testASpecialCaseDottedExpressionCanHaveTwoApplies() throws Exception {
		List<TemplateLine> tls = parseContent("x.b.c");
		assertEquals(1, tls.size());
		assertTrue(tls.get(0) instanceof ContentExpr);
		ContentExpr tl = (ContentExpr) tls.get(0);
		assertTrue("was " + tl.expr.getClass() + " not ApplyExpr", tl.expr instanceof ApplyExpr);
		ApplyExpr ae = (ApplyExpr) tl.expr;
		assertEquals(".", ae.fn.toString());
		assertEquals(2, ae.args.size());
		ApplyExpr a2 = (ApplyExpr) ae.args.get(0);
		assertEquals(".", a2.fn.toString());
		assertEquals(2, a2.args.size());
		assertEquals("x", a2.args.get(0).toString());
		assertEquals("b", a2.args.get(1).toString());
		assertEquals("c", ae.args.get(1).toString());
	}

	@Test
	public void testASpecialCaseDottedExpressionCanHaveAnArbitraryExpressionToTheLeft() throws Exception {
		List<TemplateLine> tls = parseContent("(f 3).c");
		assertEquals(1, tls.size());
		assertTrue(tls.get(0) instanceof ContentExpr);
		ContentExpr tl = (ContentExpr) tls.get(0);
		assertTrue("was " + tl.expr.getClass() + " not ApplyExpr", tl.expr instanceof ApplyExpr);
		ApplyExpr ae = (ApplyExpr) tl.expr;
		assertEquals(".", ae.fn.toString());
		assertEquals(2, ae.args.size());
		ApplyExpr a2 = (ApplyExpr) ae.args.get(0);
		assertEquals(1, a2.args.size());
		assertEquals("f", a2.fn.toString());
		assertEquals("3", a2.args.get(0).toString());
		assertEquals("c", ae.args.get(1).toString());
	}

	@Test
	public void testWeCanDefineAClickHandler() throws Exception {
		EventHandler eh = parseHandler("click => handleClick 7 (u:x)");
		assertEquals("click", eh.action);
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
	public void testReferencingAnotherTemplateByName() throws Exception {
		TemplateReference lv = (TemplateReference) parse("$inner fred 3 'string'");
		assertEquals("inner", lv.name);
		assertEquals(3, lv.args.size());
		assertTrue(lv.args.get(0) instanceof UnresolvedVar);
		assertEquals("fred", ((UnresolvedVar)lv.args.get(0)).var);
		assertTrue(lv.args.get(1) instanceof NumericLiteral);
		assertEquals("3", ((NumericLiteral)lv.args.get(1)).text);
		assertTrue(lv.args.get(2) instanceof StringLiteral);
		assertEquals("string", ((StringLiteral)lv.args.get(2)).text);
	}
	
	@Test
	public void testWeCanEditASimpleVar() throws Exception {
		List<TemplateLine> tls = parseContent("counter?");
		assertEquals(1, tls.size());
		assertUVar("counter", tls, 0);
		assertEditable(tls, 0);
		assertFormats(0, tls, 0);
	}

	@Test
	public void testWeCanEditAField() throws Exception {
		List<TemplateLine> tls = parseContent("data.counter?");
		assertEquals(1, tls.size());
		ContentExpr eh = (ContentExpr) tls.get(0);
		assertTrue(eh.expr instanceof ApplyExpr);
		ApplyExpr ae = (ApplyExpr) eh.expr;
		assertEquals(".", ae.fn.toString());
		assertEquals("data", ae.args.get(0).toString());
		assertEquals("counter", ae.args.get(1).toString());
		assertEditable(tls, 0);
		assertFormats(0, tls, 0);
	}

	@Test
	public void testWeCannotEditADiv() throws Exception {
		ErrorResult er = parseError(".?");
		assertEquals(1, er.count());
		assertEquals("div or list must be only item on line", er.get(0).msg);
	}

	@Test
	public void testWeCannotEditLineStart() throws Exception {
		ErrorResult er = parseError("? x");
		assertEquals(1, er.count());
		assertEquals("cannot have edit marker at start of line", er.get(0).msg);
	}

	@Test
	public void testWeCannotEditAConstant() throws Exception {
		ErrorResult er = parseError("'hello'?");
		assertEquals(1, er.count());
		assertEquals("not an editable field", er.get(0).msg);
	}

	@Test
	public void testWeCannotEditAnApplication() throws Exception {
		ErrorResult er = parseError("(f 3)?");
		assertEquals(1, er.count());
		assertEquals("not an editable field", er.get(0).msg);
	}

	protected TemplateLine parse(String input) throws Exception {
		Object o = doparse(input);
		assertNotNull(o);
		assertTrue(o instanceof TemplateLine);
		return (TemplateLine) o;
	}

	@SuppressWarnings("unchecked")
	protected List<TemplateLine> parseContent(String input) throws Exception {
		Object o = doparse(input);
		assertNotNull(o);
		assertTrue("was " + o.getClass() + " not List<Content>", o instanceof List);
		return (List<TemplateLine>) o;
	}

	protected TemplateDiv parseDiv(String input) throws Exception {
		Object o = doparse(input);
		assertNotNull(o);
		if (o instanceof ErrorResult)
			fail(((ErrorResult)o).singleString());
		assertTrue("was " + o.getClass() + " not TemplateDiv", o instanceof TemplateDiv);
		return (TemplateDiv) o;
	}

	protected TemplateList parseList(String input) throws Exception {
		Object o = doparse(input);
		assertNotNull(o);
		if (o instanceof ErrorResult)
			fail(((ErrorResult)o).singleString());
		assertTrue(o.toString(), o instanceof TemplateList);
		return (TemplateList) o;
	}

	protected EventHandler parseHandler(String input) throws Exception {
		Object o = doparse(input);
		assertNotNull(o);
		if (o instanceof ErrorResult)
			fail(((ErrorResult)o).singleString());
		assertTrue(o instanceof EventHandler);
		return (EventHandler) o;
	}

	protected ErrorResult parseError(String string) throws Exception {
		Object o = doparse(string);
		assertNotNull(o);
		assertTrue("was " + o.getClass() + " when expecting Error", o instanceof ErrorResult);
		return (ErrorResult) o;
	}

	private Object doparse(String string) throws Exception {
		Object ret = p.tryParsing(new Tokenizable(string));
		if (ret instanceof ErrorResult)
			((ErrorResult)ret).showTo(new PrintWriter(System.out), 0);
		return ret;

	}

	private void assertUVar(String uvar, List<TemplateLine> tls, int pos) {
		assertTrue(tls.get(pos) instanceof ContentExpr);
		ContentExpr tl = (ContentExpr) tls.get(pos);
		assertTrue(tl.expr instanceof UnresolvedVar);
		assertEquals(uvar, ((UnresolvedVar)tl.expr).var);
	}

	private void assertEditable(List<TemplateLine> tls, int pos) {
		assertTrue(tls.get(pos) instanceof ContentExpr);
		ContentExpr tl = (ContentExpr) tls.get(pos);
		assertTrue(tl.editable());
	}

	private void assertString(String str, List<TemplateLine> tls, int pos) {
		assertTrue(tls.get(pos) instanceof ContentString);
		ContentString tl = (ContentString) tls.get(pos);
		assertEquals(str, tl.text);
	}

	private void assertFormats(int count, List<TemplateLine> tls, int pos) {
		assertTrue(tls.get(pos) instanceof TemplateFormat);
		TemplateFormat tl = (TemplateFormat) tls.get(pos);
		assertEquals(count, tl.formats.size());
	}

	private void assertFormat(String label, TemplateFormat tl, int which) {
		TemplateToken tt = (TemplateToken) tl.formats.get(which);
		assertEquals(TemplateToken.STRING, tt.type);
		assertEquals(label, tt.text);
	}

	private void assertFormat(String label, List<TemplateLine> tls, int pos, int which) {
		assertTrue(tls.get(pos) instanceof TemplateFormat);
		TemplateFormat tl = (TemplateFormat) tls.get(pos);
		assertFormat(label, tl, which);
	}

	private void assertFormatVar(String var, List<TemplateLine> tls, int pos, int which) {
		assertTrue(tls.get(pos) instanceof ContentExpr);
		ContentExpr tl = (ContentExpr) tls.get(pos);
		TemplateToken tt = (TemplateToken) tl.formats.get(which);
		assertEquals(TemplateToken.IDENTIFIER, tt.type);
		assertEquals(var, tt.text);
	}
}
