package org.flasck.flas.dom;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flasck.flas.errors.ErrorResult;
import org.flasck.flas.hsie.HSIE;
import org.flasck.flas.parsedForm.ApplyExpr;
import org.flasck.flas.parsedForm.CardDefinition;
import org.flasck.flas.parsedForm.FunctionCaseDefn;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.PackageDefn;
import org.flasck.flas.parsedForm.Scope;
import org.flasck.flas.parsedForm.StateDefinition;
import org.flasck.flas.parsedForm.StringLiteral;
import org.flasck.flas.parsedForm.StructField;
import org.flasck.flas.parsedForm.Template;
import org.flasck.flas.parsedForm.TemplateLine;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.parsedForm.VarPattern;
import org.flasck.flas.parser.TemplateLineParser;
import org.flasck.flas.rewriter.Rewriter;
import org.flasck.flas.stories.Builtin;
import org.flasck.flas.tokenizers.Tokenizable;
import org.flasck.flas.vcode.hsieForm.HSIEForm;
import org.flasck.flas.vcode.hsieForm.HSIEForm.Type;
import org.junit.Ignore;
import org.junit.Test;

public class DomFunctionTests {
	private final TemplateLineParser p = new TemplateLineParser();
	private DomFunctionGenerator gen;
	private ErrorResult errors = new ErrorResult();

	@Test
	public void testAString() throws Exception {
		CardDefinition card = new CardDefinition(Builtin.builtinScope(), "MyCard");
		FunctionDefinition node1 = generateOne(card, "'hello'");
		assertEquals("MyCard._templateNode_1", node1.name);
		assertEquals(0, node1.nargs);
		assertEquals(1, node1.cases.size());
		FunctionCaseDefn fcd = node1.cases.get(0);
		assertEquals("MyCard._templateNode_1", fcd.intro.name);
		assertEquals(0, fcd.intro.args.size());
		assertNotNull(fcd.expr);
		assertEquals("\"hello\"", fcd.expr.toString());
		HSIEForm c = new HSIE(errors).handle(node1);
		c.dump();
	}

	@Test
	public void testSimpleVar() throws Exception {
		CardDefinition card = new CardDefinition(Builtin.builtinScope(), "MyCard");
		card.state = counterState();
		FunctionDefinition node1 = generateOne(card, "counter");
		assertEquals("MyCard._templateNode_1", node1.name);
		assertEquals(0, node1.nargs);
		assertEquals(1, node1.cases.size());
		FunctionCaseDefn fcd = node1.cases.get(0);
		assertEquals("MyCard._templateNode_1", fcd.intro.name);
		assertEquals(0, fcd.intro.args.size());
		assertEquals("Card[MyCard.counter]", fcd.expr.toString());
		HSIEForm c = new HSIE(errors).handle(node1);
		c.dump();
	}

	@Test
	public void testAMinimalDiv() throws Exception {
		CardDefinition card = new CardDefinition(Builtin.builtinScope(), "MyCard");
		FunctionDefinition node1 = generateOne(card, ".");
		assertEquals("MyCard._templateNode_1", node1.name);
		assertEquals(0, node1.nargs);
		assertEquals(1, node1.cases.size());
		FunctionCaseDefn fcd = node1.cases.get(0);
		assertEquals("MyCard._templateNode_1", fcd.intro.name);
		assertEquals(0, fcd.intro.args.size());
		assertNotNull(fcd.expr);
		assertTrue(fcd.expr instanceof ApplyExpr);
		ApplyExpr ae = (ApplyExpr) fcd.expr;
		assertEquals("DOM.Element", ae.fn.toString());
		assertEquals(4, ae.args.size());
		assertEquals("\"div\"", ae.args.get(0).toString());
		assertEquals("Nil", ae.args.get(1).toString());
		assertEquals("Nil", ae.args.get(2).toString());
		assertEquals("Nil", ae.args.get(3).toString());
		HSIEForm c = new HSIE(errors).handle(node1);
		c.dump();
	}

	@Test
	public void testATaggedDiv() throws Exception {
		CardDefinition card = new CardDefinition(Builtin.builtinScope(), "MyCard");
		FunctionDefinition node1 = generateOne(card, "#nav");
		assertEquals("MyCard._templateNode_1", node1.name);
		assertEquals(0, node1.nargs);
		assertEquals(1, node1.cases.size());
		FunctionCaseDefn fcd = node1.cases.get(0);
		assertEquals("MyCard._templateNode_1", fcd.intro.name);
		assertEquals(0, fcd.intro.args.size());
		assertNotNull(fcd.expr);
		assertTrue(fcd.expr instanceof ApplyExpr);
		ApplyExpr ae = (ApplyExpr) fcd.expr;
		assertEquals("DOM.Element", ae.fn.toString());
		assertEquals(4, ae.args.size());
		assertEquals("\"nav\"", ae.args.get(0).toString());
		assertEquals("Nil", ae.args.get(1).toString());
		assertEquals("Nil", ae.args.get(2).toString());
		assertEquals("Nil", ae.args.get(3).toString());
		HSIEForm c = new HSIE(errors).handle(node1);
		c.dump();
	}

	@Test
	@Ignore // this needs more rewriting to make it work
	public void testCallingAFunction() throws Exception {
		Scope biscope = Builtin.builtinScope();
		PackageDefn pd = new PackageDefn(biscope, "ME");
		Scope scope = pd.innerScope();
		List<Object> args = new ArrayList<Object>();
		args.add(new VarPattern("ignore"));
		List<FunctionCaseDefn> fcds = new ArrayList<FunctionCaseDefn>();
		FunctionCaseDefn fcd1 = new FunctionCaseDefn(scope, "ME.f", args, new StringLiteral("hello"));
		fcds.add(fcd1);
		FunctionDefinition tfn = new FunctionDefinition(Type.FUNCTION, fcd1.intro, fcds);
		scope.define("tfn", "tfn", tfn);
		CardDefinition card = new CardDefinition(scope, "MyCard");
		card.state = new StateDefinition();
		card.state.fields.add(new StructField(new TypeReference(null, "Number", null), "counter"));
//		scope.define("MyCard", "MyCard", card);
		FunctionDefinition node1 = generateOne(card, "(tfn counter)");
		card.innerScope().define("_templateNode_1", "ME.MyCard._templateNode_1", node1);

		assertEquals("MyCard._templateNode_1", node1.name);
		assertEquals(0, node1.nargs);
		assertEquals(1, node1.cases.size());
		FunctionCaseDefn fcd = node1.cases.get(0);
		assertEquals("MyCard._templateNode_1", fcd.intro.name);
		assertEquals(0, fcd.intro.args.size());
		assertNotNull(fcd.expr);
		assertTrue(fcd.expr instanceof ApplyExpr);
		ApplyExpr ae = (ApplyExpr) fcd.expr;
		assertEquals("tfn", ae.fn.toString());
		assertEquals(1, ae.args.size());
		assertEquals("counter", ae.args.get(0).toString());
		Rewriter rewriter = new Rewriter(new ErrorResult());
		rewriter.rewrite(pd.myEntry());
		HSIEForm c = new HSIE(errors).handle(rewriter.functions.get("ME.MyCard._templateNode_1"));
		c.dump();
	}

	private FunctionDefinition generateOne(CardDefinition card, String input) throws Exception {
		Map<String, FunctionDefinition> functions = new HashMap<String, FunctionDefinition>();
		TemplateLine tl = parse(input);
		Template t = card.template;
		if (t == null)
			t = new Template(card.name, tl, card.innerScope());
		gen = new DomFunctionGenerator(errors, t, functions);
		gen.generateOne(tl, "0.0");
		assertEquals(1, functions.size());
		FunctionDefinition ret = functions.get("MyCard._templateNode_1");
		assertNotNull(ret);
		return ret;
	}

	private StateDefinition counterState() {
		StateDefinition sd = new StateDefinition();
		sd.fields.add(new StructField(new TypeReference(null, "Number", null), "counter"));
		return sd;
	}

	protected TemplateLine parse(String input) throws Exception {
		Object ret = p.tryParsing(new Tokenizable(input));
		if (ret instanceof ErrorResult)
			((ErrorResult)ret).showTo(new PrintWriter(System.out), 0);
		assertNotNull(ret);
		assertTrue(ret instanceof TemplateLine);
		return (TemplateLine) ret;
	}


}
