package org.flasck.flas.dom;

import static org.junit.Assert.*;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import org.flasck.flas.ErrorResult;
import org.flasck.flas.hsie.HSIE;
import org.flasck.flas.parsedForm.ApplyExpr;
import org.flasck.flas.parsedForm.FunctionCaseDefn;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.ItemExpr;
import org.flasck.flas.parsedForm.StateDefinition;
import org.flasck.flas.parsedForm.StructField;
import org.flasck.flas.parsedForm.TemplateLine;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.parsedForm.VarPattern;
import org.flasck.flas.parser.TemplateLineParser;
import org.flasck.flas.tokenizers.Tokenizable;
import org.flasck.flas.vcode.hsieForm.HSIEForm;
import org.junit.Test;

public class DomFunctionTests {
	private final TemplateLineParser p = new TemplateLineParser();
	private DomFunctionGenerator gen;

	@Test
	public void testAString() throws Exception {
		FunctionDefinition node1 = generateOne(null, "'hello'");
		assertEquals("_templateNode_1", node1.name);
		assertEquals(0, node1.nargs);
		assertEquals(1, node1.cases.size());
		FunctionCaseDefn fcd = node1.cases.get(0);
		assertEquals("_templateNode_1", fcd.intro.name);
		assertEquals(0, fcd.intro.args.size());
		assertNotNull(fcd.expr);
		assertTrue(fcd.expr instanceof ItemExpr);
		assertEquals("hello", ((ItemExpr) fcd.expr).tok.text);
		HSIEForm c = HSIE.handle(node1);
		c.dump();
	}

	@Test
	public void testSimpleVar() throws Exception {
		FunctionDefinition node1 = generateOne(counterState(), "counter");
		assertEquals("_templateNode_1", node1.name);
		assertEquals(0, node1.nargs);
		assertEquals(1, node1.cases.size());
		FunctionCaseDefn fcd = node1.cases.get(0);
		assertEquals("_templateNode_1", fcd.intro.name);
		assertEquals(0, fcd.intro.args.size());
		assertTrue(fcd.expr instanceof ItemExpr);
		assertEquals("counter", ((ItemExpr)fcd.expr).tok.text);
		HSIEForm c = HSIE.handle(node1);
		c.dump();
	}

	@Test
	public void testAMinimalDiv() throws Exception {
		FunctionDefinition node1 = generateOne(null, ".");
		assertEquals("_templateNode_1", node1.name);
		assertEquals(0, node1.nargs);
		assertEquals(1, node1.cases.size());
		FunctionCaseDefn fcd = node1.cases.get(0);
		assertEquals("_templateNode_1", fcd.intro.name);
		assertEquals(0, fcd.intro.args.size());
		assertNotNull(fcd.expr);
		assertTrue(fcd.expr instanceof ApplyExpr);
		ApplyExpr ae = (ApplyExpr) fcd.expr;
		assertEquals("DOM.Element", ((ItemExpr)ae.fn).tok.text);
		assertEquals(3, ae.args.size());
		assertEquals("div", ((ItemExpr)ae.args.get(0)).tok.text);
		assertEquals("Nil", ((ItemExpr)ae.args.get(1)).tok.text);
		assertEquals("Nil", ((ItemExpr)ae.args.get(2)).tok.text);
		HSIEForm c = HSIE.handle(node1);
		c.dump();
	}

	@Test
	public void testATaggedDiv() throws Exception {
		FunctionDefinition node1 = generateOne(null, "#nav");
		assertEquals("_templateNode_1", node1.name);
		assertEquals(0, node1.nargs);
		assertEquals(1, node1.cases.size());
		FunctionCaseDefn fcd = node1.cases.get(0);
		assertEquals("_templateNode_1", fcd.intro.name);
		assertEquals(0, fcd.intro.args.size());
		assertNotNull(fcd.expr);
		assertTrue(fcd.expr instanceof ApplyExpr);
		ApplyExpr ae = (ApplyExpr) fcd.expr;
		assertEquals("DOM.Element", ((ItemExpr)ae.fn).tok.text);
		assertEquals(3, ae.args.size());
		assertEquals("nav", ((ItemExpr)ae.args.get(0)).tok.text);
		assertEquals("Nil", ((ItemExpr)ae.args.get(1)).tok.text);
		assertEquals("Nil", ((ItemExpr)ae.args.get(2)).tok.text);
		HSIEForm c = HSIE.handle(node1);
		c.dump();
	}

	@Test
	public void testCallingAFunction() throws Exception {
		FunctionDefinition node1 = generateOne(null, "(tfn counter)");
		assertEquals("_templateNode_1", node1.name);
		assertEquals(0, node1.nargs);
		assertEquals(1, node1.cases.size());
		FunctionCaseDefn fcd = node1.cases.get(0);
		assertEquals("_templateNode_1", fcd.intro.name);
		assertEquals(0, fcd.intro.args.size());
		assertNotNull(fcd.expr);
		assertTrue(fcd.expr instanceof ApplyExpr);
		ApplyExpr ae = (ApplyExpr) fcd.expr;
		assertEquals("tfn", ((ItemExpr)ae.fn).tok.text);
		assertEquals(1, ae.args.size());
		assertEquals("counter", ((ItemExpr)ae.args.get(0)).tok.text);
		HSIEForm c = HSIE.handle(node1);
		c.dump();
	}

	private FunctionDefinition generateOne(StateDefinition state, String input) throws Exception {
		Map<String, FunctionDefinition> functions = new HashMap<String, FunctionDefinition>();
		gen = new DomFunctionGenerator(functions, null, state);
		TemplateLine tl = parse(input);
		gen.generateOne(tl);
		assertEquals(1, functions.size());
		FunctionDefinition ret = functions.get("_templateNode_1");
		assertNotNull(ret);
		return ret;
	}

	private StateDefinition counterState() {
		StateDefinition sd = new StateDefinition();
		sd.fields.add(new StructField(new TypeReference("Number"), "counter"));
		return sd;
	}

	protected TemplateLine parse(String input) throws Exception {
		Object ret = p.tryParsing(new Tokenizable(input));
		if (ret instanceof ErrorResult)
			((ErrorResult)ret).showTo(new PrintWriter(System.out));
		assertNotNull(ret);
		assertTrue(ret instanceof TemplateLine);
		return (TemplateLine) ret;
	}


}
