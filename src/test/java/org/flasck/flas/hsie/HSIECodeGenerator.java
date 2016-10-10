package org.flasck.flas.hsie;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;

import org.flasck.flas.errors.ErrorResult;
import org.flasck.flas.parsedForm.ApplyExpr;
import org.flasck.flas.parsedForm.FunctionCaseDefn;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.LetExpr;
import org.flasck.flas.parsedForm.NumericLiteral;
import org.flasck.flas.parsedForm.PackageDefn;
import org.flasck.flas.parsedForm.Scope;
import org.flasck.flas.parser.FunctionParser;
import org.flasck.flas.rewriter.Rewriter;
import org.flasck.flas.rewrittenForm.LocalVar;
import org.flasck.flas.rewrittenForm.PackageVar;
import org.flasck.flas.rewrittenForm.RWFunctionCaseDefn;
import org.flasck.flas.rewrittenForm.RWFunctionDefinition;
import org.flasck.flas.rewrittenForm.RWFunctionIntro;
import org.flasck.flas.stories.Builtin;
import org.flasck.flas.stories.FLASStory;
import org.flasck.flas.tokenizers.Tokenizable;
import org.flasck.flas.vcode.hsieForm.HSIEForm;
import org.flasck.flas.vcode.hsieForm.HSIEForm.CodeType;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.zinutils.collections.CollectionUtils;

// Although these are tests, they are really just to make sure that the data
// we enter in HSIETestData is valid from programs.
public class HSIECodeGenerator {
	private ErrorResult errors = new ErrorResult();
	
	@Test
	public void testConvertingIdOf1() throws Exception {
		Scope biscope = Builtin.builtinScope();
		PackageDefn pkg = new PackageDefn(null, biscope, "ME");
		pkg.myEntry().scope().define("plus1", "plus1", null);
		FunctionParser p = new FunctionParser(new FLASStory.State(null, "ME", HSIEForm.CodeType.FUNCTION));
		FunctionCaseDefn c1 = (FunctionCaseDefn)p.tryParsing(new Tokenizable("f = plus1 1"));
		FunctionDefinition f = new FunctionDefinition(null, CodeType.FUNCTION, "ME.f", 0, CollectionUtils.listOf(c1));
		pkg.innerScope().define("f", "ME.f", f);
		Rewriter rw = new Rewriter(errors, null);
		rw.rewrite(pkg.myEntry());
		errors.showTo(new PrintWriter(System.out), 0);
		assertEquals(0, errors.count());
		System.out.println(rw.functions);
		HSIEForm form = new HSIE(errors, rw, biscope).handle(null, rw.functions.get("ME.f"));
		assertNotNull(form);
		HSIETestData.assertHSIE(HSIETestData.plus1Of1(), form);
	}

	// This is a pathological case of LET with vars
	@Test
	public void testConvertingIdDecode() throws Exception {
		Scope biscope = Builtin.builtinScope();
		PackageDefn pkg = new PackageDefn(null, biscope, "ME");
		pkg.myEntry().scope().define("id", "id", null);
		pkg.myEntry().scope().define("decode", "decode", null);
		FunctionParser p = new FunctionParser(new FLASStory.State(null, "ME", HSIEForm.CodeType.FUNCTION));
		FunctionCaseDefn c1 = (FunctionCaseDefn)p.tryParsing(new Tokenizable("f = id (decode (id 32))"));
		FunctionDefinition f = new FunctionDefinition(null, CodeType.FUNCTION, "ME.f", 0, CollectionUtils.listOf(c1));
		pkg.innerScope().define("f", "ME.f", f);
		Rewriter rw = new Rewriter(errors, null);
		rw.rewrite(pkg.myEntry());
		errors.showTo(new PrintWriter(System.out), 0);
		assertEquals(0, errors.count());
		System.out.println(rw.functions);
		HSIEForm form = new HSIE(errors, rw, biscope).handle(null, rw.functions.get("ME.f"));
		assertNotNull(form);
		HSIETestData.assertHSIE(HSIETestData.idDecode(), form);
	}

	@Test
	@Ignore
	public void testPatternMatchingAPolyVar() throws Exception {
		Scope biscope = Builtin.builtinScope();
		PackageDefn pkg = new PackageDefn(null, biscope, "ME");
		FunctionParser p = new FunctionParser(new FLASStory.State(null, "ME", HSIEForm.CodeType.FUNCTION));
		FunctionCaseDefn c1 = (FunctionCaseDefn)p.tryParsing(new Tokenizable("push (Cons[A] x) (A y) = Cons y x"));
		FunctionDefinition f = new FunctionDefinition(null, CodeType.FUNCTION, "ME.push", 1, CollectionUtils.listOf(c1));
		pkg.innerScope().define("f", "ME.push", f);
		Rewriter rw = new Rewriter(errors, null);
		rw.rewrite(pkg.myEntry());
		errors.showTo(new PrintWriter(System.out), 0);
		assertEquals(errors.singleString(), 0, errors.count());
		System.out.println(rw.functions);
		HSIEForm form = new HSIE(errors, rw, biscope).handle(null, rw.functions.get("ME.push"));
		assertNotNull(form);
		form.dump((Logger)null);
		HSIETestData.assertHSIE(HSIETestData.unionType(), form);
	}

	@Test
	public void testPatternMatchingAUnionType() throws Exception {
		Scope biscope = Builtin.builtinScope();
		PackageDefn pkg = new PackageDefn(null, biscope, "ME");
		FunctionParser p = new FunctionParser(new FLASStory.State(null, "ME", HSIEForm.CodeType.FUNCTION));
		FunctionCaseDefn c1 = (FunctionCaseDefn)p.tryParsing(new Tokenizable("f (List[A] x) = 10"));
		FunctionDefinition f = new FunctionDefinition(null, CodeType.FUNCTION, "ME.f", 1, CollectionUtils.listOf(c1));
		pkg.innerScope().define("f", "ME.f", f);
		Rewriter rw = new Rewriter(errors, null);
		rw.rewrite(pkg.myEntry());
		errors.showTo(new PrintWriter(System.out), 0);
		assertEquals(errors.singleString(), 0, errors.count());
		System.out.println(rw.functions);
		HSIEForm form = new HSIE(errors, rw, biscope).handle(null, rw.functions.get("ME.f"));
		assertNotNull(form);
		form.dump((Logger)null);
		HSIETestData.assertHSIE(HSIETestData.unionType(), form);
	}

	@Test
	public void testASimpleRecursivelyDefinedFunction1() throws Exception {
		Scope biscope = Builtin.builtinScope();
		PackageDefn pkg = new PackageDefn(null, biscope, "ME");
		FunctionParser p = new FunctionParser(new FLASStory.State(null, "ME", HSIEForm.CodeType.FUNCTION));
		FunctionCaseDefn c1 = (FunctionCaseDefn)p.tryParsing(new Tokenizable("f x = g (x-1)"));
		FunctionDefinition f = new FunctionDefinition(null, CodeType.FUNCTION, "ME.f", 1, CollectionUtils.listOf(c1));
		FunctionCaseDefn g1 = (FunctionCaseDefn)p.tryParsing(new Tokenizable("g x = f (x+1)"));
		FunctionDefinition g = new FunctionDefinition(null, CodeType.FUNCTION, "ME.g", 1, CollectionUtils.listOf(g1));
		pkg.innerScope().define("f", "ME.f", f);
		pkg.innerScope().define("g", "ME.g", g);
		Rewriter rw = new Rewriter(errors, null);
		rw.rewrite(pkg.myEntry());
		errors.showTo(new PrintWriter(System.out), 0);
		assertEquals(errors.singleString(), 0, errors.count());
		System.out.println(rw.functions);
		HSIEForm form = new HSIE(errors, rw, biscope).handle(null, rw.functions.get("ME.f"));
		assertNotNull(form);
		form.dump((Logger)null);
		HSIETestData.assertHSIE(HSIETestData.rdf1(), form);
	}

	@Test
	public void testASimpleRecursivelyDefinedFunction2() throws Exception {
		Scope biscope = Builtin.builtinScope();
		PackageDefn pkg = new PackageDefn(null, biscope, "ME");
		FunctionParser p = new FunctionParser(new FLASStory.State(null, "ME", HSIEForm.CodeType.FUNCTION));
		FunctionCaseDefn c1 = (FunctionCaseDefn)p.tryParsing(new Tokenizable("f x = g (x-1)"));
		FunctionDefinition f = new FunctionDefinition(null, CodeType.FUNCTION, "ME.f", 1, CollectionUtils.listOf(c1));
		FunctionCaseDefn g1 = (FunctionCaseDefn)p.tryParsing(new Tokenizable("g x = f (x+1)"));
		FunctionDefinition g = new FunctionDefinition(null, CodeType.FUNCTION, "ME.g", 1, CollectionUtils.listOf(g1));
		pkg.innerScope().define("f", "ME.f", f);
		pkg.innerScope().define("g", "ME.g", g);
		Rewriter rw = new Rewriter(errors, null);
		rw.rewrite(pkg.myEntry());
		errors.showTo(new PrintWriter(System.out), 0);
		assertEquals(errors.singleString(), 0, errors.count());
		System.out.println(rw.functions);
		HSIEForm form = new HSIE(errors, rw, biscope).handle(null, rw.functions.get("ME.g"));
		assertNotNull(form);
		HSIETestData.assertHSIE(HSIETestData.rdf2(), form);
	}

	@Test
	public void testADirectLet() throws Exception {
		Scope biscope = Builtin.builtinScope();
		PackageDefn pkg = new PackageDefn(null, biscope, "ME");
		LetExpr expr = new LetExpr("_x",
					new ApplyExpr(null, new PackageVar(null, "FLEval.plus", null), new NumericLiteral(null, "2"), new NumericLiteral(null, "2")),
					new ApplyExpr(null, new PackageVar(null, "FLEval.plus", null), new LocalVar("ME.f", null, "_x", null, null), new LocalVar("ME.f", null, "_x", null, null)));
		RWFunctionCaseDefn fcd = new RWFunctionCaseDefn(new RWFunctionIntro(null, "ME.f", new ArrayList<>(), new HashMap<>()), expr);
		RWFunctionDefinition f = new RWFunctionDefinition(null, CodeType.FUNCTION, fcd.intro, CollectionUtils.listOf(fcd));
		HSIEForm form = new HSIE(errors, null, biscope).handle(null, f);
		assertNotNull(form);
		HSIETestData.assertHSIE(HSIETestData.directLet(), form);
	}
}
