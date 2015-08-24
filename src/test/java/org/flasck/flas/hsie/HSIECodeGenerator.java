package org.flasck.flas.hsie;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.PrintWriter;
import java.util.ArrayList;

import org.flasck.flas.errors.ErrorResult;
import org.flasck.flas.parsedForm.AbsoluteVar;
import org.flasck.flas.parsedForm.ApplyExpr;
import org.flasck.flas.parsedForm.FunctionCaseDefn;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.LetExpr;
import org.flasck.flas.parsedForm.LocalVar;
import org.flasck.flas.parsedForm.NumericLiteral;
import org.flasck.flas.parsedForm.PackageDefn;
import org.flasck.flas.parser.FunctionParser;
import org.flasck.flas.rewriter.Rewriter;
import org.flasck.flas.stories.Builtin;
import org.flasck.flas.stories.FLASStory;
import org.flasck.flas.tokenizers.Tokenizable;
import org.flasck.flas.vcode.hsieForm.HSIEForm;
import org.flasck.flas.vcode.hsieForm.HSIEForm.Type;
import org.junit.Test;
import org.zinutils.collections.CollectionUtils;

// Although these are tests, they are really just to make sure that the data
// we enter in HSIETestData is valid from programs.
public class HSIECodeGenerator {
	private ErrorResult errors = new ErrorResult();
	
	@Test
	public void testConvertingIdOf1() throws Exception {
		PackageDefn pkg = new PackageDefn(null, Builtin.builtinScope(), "ME");
		pkg.myEntry().scope().define("plus1", "plus1", null);
		FunctionParser p = new FunctionParser(new FLASStory.State(null, "ME", HSIEForm.Type.FUNCTION));
		FunctionCaseDefn c1 = (FunctionCaseDefn)p.tryParsing(new Tokenizable("f = plus1 1"));
		FunctionDefinition f = new FunctionDefinition(null, Type.FUNCTION, "ME.f", 0, CollectionUtils.listOf(c1));
		pkg.innerScope().define("f", "ME.f", f);
		Rewriter rw = new Rewriter(errors, null);
		rw.rewrite(pkg.myEntry());
		errors.showTo(new PrintWriter(System.out), 0);
		assertEquals(0, errors.errors.size());
		System.out.println(rw.functions);
		HSIEForm form = new HSIE(errors).handle(rw.functions.get("ME.f"));
		assertNotNull(form);
		HSIETestData.assertHSIE(HSIETestData.plus1Of1(), form);
	}

	// This is a pathological case of LET with vars
	@Test
	public void testConvertingIdDecode() throws Exception {
		PackageDefn pkg = new PackageDefn(null, Builtin.builtinScope(), "ME");
		pkg.myEntry().scope().define("id", "id", null);
		pkg.myEntry().scope().define("decode", "decode", null);
		FunctionParser p = new FunctionParser(new FLASStory.State(null, "ME", HSIEForm.Type.FUNCTION));
		FunctionCaseDefn c1 = (FunctionCaseDefn)p.tryParsing(new Tokenizable("f = id (decode (id 32))"));
		FunctionDefinition f = new FunctionDefinition(null, Type.FUNCTION, "ME.f", 0, CollectionUtils.listOf(c1));
		pkg.innerScope().define("f", "ME.f", f);
		Rewriter rw = new Rewriter(errors, null);
		rw.rewrite(pkg.myEntry());
		errors.showTo(new PrintWriter(System.out), 0);
		assertEquals(0, errors.errors.size());
		System.out.println(rw.functions);
		HSIEForm form = new HSIE(errors).handle(rw.functions.get("ME.f"));
		assertNotNull(form);
		HSIETestData.assertHSIE(HSIETestData.idDecode(), form);
	}

	@Test
	public void testASimpleRecursivelyDefinedFunction1() throws Exception {
		PackageDefn pkg = new PackageDefn(null, Builtin.builtinScope(), "ME");
		FunctionParser p = new FunctionParser(new FLASStory.State(null, "ME", HSIEForm.Type.FUNCTION));
		FunctionCaseDefn c1 = (FunctionCaseDefn)p.tryParsing(new Tokenizable("f x = g (x-1)"));
		FunctionDefinition f = new FunctionDefinition(null, Type.FUNCTION, "ME.f", 1, CollectionUtils.listOf(c1));
		FunctionCaseDefn g1 = (FunctionCaseDefn)p.tryParsing(new Tokenizable("g x = f (x+1)"));
		FunctionDefinition g = new FunctionDefinition(null, Type.FUNCTION, "ME.g", 1, CollectionUtils.listOf(g1));
		pkg.innerScope().define("f", "ME.f", f);
		pkg.innerScope().define("g", "ME.g", g);
		Rewriter rw = new Rewriter(errors, null);
		rw.rewrite(pkg.myEntry());
		errors.showTo(new PrintWriter(System.out), 0);
		assertEquals(0, errors.errors.size());
		System.out.println(rw.functions);
		HSIEForm form = new HSIE(errors).handle(rw.functions.get("ME.f"));
		assertNotNull(form);
		form.dump();
		HSIETestData.assertHSIE(HSIETestData.rdf1(), form);
	}

	@Test
	public void testASimpleRecursivelyDefinedFunction2() throws Exception {
		PackageDefn pkg = new PackageDefn(null, Builtin.builtinScope(), "ME");
		FunctionParser p = new FunctionParser(new FLASStory.State(null, "ME", HSIEForm.Type.FUNCTION));
		FunctionCaseDefn c1 = (FunctionCaseDefn)p.tryParsing(new Tokenizable("f x = g (x-1)"));
		FunctionDefinition f = new FunctionDefinition(null, Type.FUNCTION, "ME.f", 1, CollectionUtils.listOf(c1));
		FunctionCaseDefn g1 = (FunctionCaseDefn)p.tryParsing(new Tokenizable("g x = f (x+1)"));
		FunctionDefinition g = new FunctionDefinition(null, Type.FUNCTION, "ME.g", 1, CollectionUtils.listOf(g1));
		pkg.innerScope().define("f", "ME.f", f);
		pkg.innerScope().define("g", "ME.g", g);
		Rewriter rw = new Rewriter(errors, null);
		rw.rewrite(pkg.myEntry());
		errors.showTo(new PrintWriter(System.out), 0);
		assertEquals(0, errors.errors.size());
		System.out.println(rw.functions);
		HSIEForm form = new HSIE(errors).handle(rw.functions.get("ME.g"));
		assertNotNull(form);
		HSIETestData.assertHSIE(HSIETestData.rdf2(), form);
	}

	@Test
	public void testADirectLet() throws Exception {
		PackageDefn pkg = new PackageDefn(null, Builtin.builtinScope(), "ME");
		LetExpr expr = new LetExpr("_x",
					new ApplyExpr(null, new AbsoluteVar(null, "FLEval.plus", null), new NumericLiteral(null, "2"), new NumericLiteral(null, "2")),
					new ApplyExpr(null, new AbsoluteVar(null, "FLEval.plus", null), new LocalVar(null, "ME.f", "_x"), new LocalVar(null, "ME.f", "_x")));
		FunctionCaseDefn fcd = new FunctionCaseDefn(pkg.innerScope(), null, "ME.f", new ArrayList<Object>(), expr);
		FunctionDefinition f = new FunctionDefinition(null, Type.FUNCTION, fcd.intro, CollectionUtils.listOf(fcd));
		HSIEForm form = new HSIE(errors).handle(f);
		assertNotNull(form);
		HSIETestData.assertHSIE(HSIETestData.directLet(), form);
	}
}
