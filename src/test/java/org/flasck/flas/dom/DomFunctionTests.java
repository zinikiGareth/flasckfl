package org.flasck.flas.dom;

import static org.junit.Assert.*;

import java.io.PrintWriter;

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
		gen = new DomFunctionGenerator(null, null, counterState());
		TemplateLine tl = parse("'hello'");
		FunctionDefinition node1 = gen.generateOne(tl);
		assertNotNull(node1);
		assertEquals("_templateNode_1", node1.name);
		assertEquals(1, node1.nargs);
		assertEquals(1, node1.cases.size());
		FunctionCaseDefn fcd = node1.cases.get(0);
		assertEquals("_templateNode_1", fcd.intro.name);
		assertEquals(1, fcd.intro.args.size());
		assertTrue(fcd.intro.args.get(0) instanceof VarPattern);
		assertEquals("card", ((VarPattern)fcd.intro.args.get(0)).var);
		assertNotNull(fcd.expr);
		assertTrue(fcd.expr instanceof ItemExpr);
		assertEquals("hello", ((ItemExpr) fcd.expr).tok.text);
		HSIEForm c = HSIE.handle(node1);
		c.dump();
	}

	@Test
	public void testSimpleVar() throws Exception {
		gen = new DomFunctionGenerator(null, null, counterState());
		TemplateLine tl = parse("counter");
		FunctionDefinition node1 = gen.generateOne(tl);
		assertNotNull(node1);
		assertEquals("_templateNode_1", node1.name);
		assertEquals(1, node1.nargs);
		assertEquals(1, node1.cases.size());
		FunctionCaseDefn fcd = node1.cases.get(0);
		assertEquals("_templateNode_1", fcd.intro.name);
		assertEquals(1, fcd.intro.args.size());
		assertTrue(fcd.intro.args.get(0) instanceof VarPattern);
		assertEquals("card", ((VarPattern)fcd.intro.args.get(0)).var);
		assertTrue(fcd.expr instanceof ApplyExpr);
		ApplyExpr ae = (ApplyExpr) fcd.expr;
		assertEquals(".", ((ItemExpr)ae.fn).tok.text);
		assertEquals("card", ((ItemExpr)ae.args.get(0)).tok.text);
		assertEquals("counter", ((ItemExpr)ae.args.get(1)).tok.text);
		HSIEForm c = HSIE.handle(node1);
		c.dump();
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
