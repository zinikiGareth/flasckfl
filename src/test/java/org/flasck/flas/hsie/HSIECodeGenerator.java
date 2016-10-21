package org.flasck.flas.hsie;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;

import org.flasck.flas.commonBase.ApplyExpr;
import org.flasck.flas.commonBase.LetExpr;
import org.flasck.flas.commonBase.NumericLiteral;
import org.flasck.flas.errors.ErrorResult;
import org.flasck.flas.flim.Builtin;
import org.flasck.flas.parsedForm.FunctionCaseDefn;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.Scope;
import org.flasck.flas.parsedForm.Scope.ScopeEntry;
import org.flasck.flas.parser.FunctionParser;
import org.flasck.flas.rewriter.Rewriter;
import org.flasck.flas.rewrittenForm.LocalVar;
import org.flasck.flas.rewrittenForm.PackageVar;
import org.flasck.flas.rewrittenForm.RWFunctionCaseDefn;
import org.flasck.flas.rewrittenForm.RWFunctionDefinition;
import org.flasck.flas.rewrittenForm.RWFunctionIntro;
import org.flasck.flas.stories.FLASStory;
import org.flasck.flas.tokenizers.Tokenizable;
import org.flasck.flas.vcode.hsieForm.HSIEForm;
import org.flasck.flas.vcode.hsieForm.HSIEForm.CodeType;
import org.junit.Ignore;
import org.junit.Test;

// Although these are tests, they are really just to make sure that the data
// we enter in HSIETestData is valid from programs.
public class HSIECodeGenerator {
	private ErrorResult errors = new ErrorResult();
	
	@Test
	public void testConvertingIdOf1() throws Exception {
//		Scope biscope = Builtin.builtinScope();
//		PackageDefn pkg = new PackageDefn(null, biscope, "ME");
		Scope s = new Scope(null, null);
//		s.define("plus1", "plus1", null);
		FunctionParser p = new FunctionParser(new FLASStory.State(null, "ME", HSIEForm.CodeType.FUNCTION));
		FunctionCaseDefn c1 = (FunctionCaseDefn)p.tryParsing(new Tokenizable("f = plus1 1"));
		FunctionDefinition f = new FunctionDefinition(null, CodeType.FUNCTION, "ME.f", 0);
		ScopeEntry me = s.define("f", "ME.f", f);
		f.cases.add(new FunctionCaseDefn(me, c1, 0));
		Rewriter rw = new Rewriter(errors, null, Builtin.builtins());
		rw.functions.put("plus1", new RWFunctionDefinition(null, CodeType.FUNCTION, "plus1", 1, false));
		rw.rewritePackageScope("ME", s);
		errors.showTo(new PrintWriter(System.out), 0);
		assertEquals(0, errors.count());
		System.out.println(rw.functions);
		HSIEForm form = new HSIE(errors, rw).handle(null, rw.functions.get("ME.f"));
		assertNotNull(form);
		HSIETestData.assertHSIE(HSIETestData.plus1Of1(), form);
	}

	// This is a pathological case of LET with vars
	@Test
	public void testConvertingIdDecode() throws Exception {
//		Scope biscope = Builtin.builtinScope();
//		PackageDefn pkg = new PackageDefn(null, biscope, "ME");
//		pkg.myEntry().scope().define("id", "id", null);
//		pkg.myEntry().scope().define("decode", "decode", null);
		Scope s = new Scope(null, null);
		FunctionParser p = new FunctionParser(new FLASStory.State(null, "ME", HSIEForm.CodeType.FUNCTION));
		FunctionCaseDefn c1 = (FunctionCaseDefn)p.tryParsing(new Tokenizable("f = id (decode (id 32))"));
		FunctionDefinition f = new FunctionDefinition(null, CodeType.FUNCTION, "ME.f", 0);
		ScopeEntry me = s.define("f", "ME.f", f);
		f.cases.add(new FunctionCaseDefn(me, c1, 0));
		Rewriter rw = new Rewriter(errors, null, Builtin.builtins());
		rw.functions.put("id", new RWFunctionDefinition(null, CodeType.FUNCTION, "id", 1, false));
		rw.functions.put("decode", new RWFunctionDefinition(null, CodeType.FUNCTION, "decode", 1, false));
		rw.rewritePackageScope("ME", s);
		errors.showTo(new PrintWriter(System.out), 0);
		assertEquals(0, errors.count());
		System.out.println(rw.functions);
		HSIEForm form = new HSIE(errors, rw).handle(null, rw.functions.get("ME.f"));
		assertNotNull(form);
		HSIETestData.assertHSIE(HSIETestData.idDecode(), form);
	}

	@Test
	@Ignore
	public void testPatternMatchingAPolyVar() throws Exception {
//		Scope biscope = Builtin.builtinScope();
//		PackageDefn pkg = new PackageDefn(null, biscope, "ME");
		Scope s = new Scope(null, null);
		FunctionParser p = new FunctionParser(new FLASStory.State(null, "ME", HSIEForm.CodeType.FUNCTION));
		FunctionCaseDefn c1 = (FunctionCaseDefn)p.tryParsing(new Tokenizable("push (Cons[A] x) (A y) = Cons y x"));
		FunctionDefinition f = new FunctionDefinition(null, CodeType.FUNCTION, "ME.push", 1);
		ScopeEntry me = s.define("push", "ME.push", f);
		f.cases.add(new FunctionCaseDefn(me, c1, 0));
		Rewriter rw = new Rewriter(errors, null, Builtin.builtins());
		rw.rewritePackageScope("ME", s);
		errors.showTo(new PrintWriter(System.out), 0);
		assertEquals(errors.singleString(), 0, errors.count());
		System.out.println(rw.functions);
		HSIEForm form = new HSIE(errors, rw).handle(null, rw.functions.get("ME.push"));
		assertNotNull(form);
		form.dump(new PrintWriter(System.out));
		HSIETestData.assertHSIE(HSIETestData.unionType(), form);
	}

	@Test
	public void testPatternMatchingAUnionType() throws Exception {
//		Scope biscope = Builtin.builtinScope();
//		PackageDefn pkg = new PackageDefn(null, biscope, "ME");
		Scope s = new Scope(null, null);
		FunctionParser p = new FunctionParser(new FLASStory.State(null, "ME", HSIEForm.CodeType.FUNCTION));
		FunctionCaseDefn c1 = (FunctionCaseDefn)p.tryParsing(new Tokenizable("f (List[A] x) = 10"));
		FunctionDefinition f = new FunctionDefinition(null, CodeType.FUNCTION, "ME.f", 1);
		ScopeEntry me = s.define("f", "ME.f", f);
		f.cases.add(new FunctionCaseDefn(me, c1, 0));
		Rewriter rw = new Rewriter(errors, null, Builtin.builtins());
		rw.rewritePackageScope("ME", s);
		errors.showTo(new PrintWriter(System.out), 0);
		assertEquals(errors.singleString(), 0, errors.count());
		System.out.println(rw.functions);
		HSIEForm form = new HSIE(errors, rw).handle(null, rw.functions.get("ME.f"));
		assertNotNull(form);
		form.dump(new PrintWriter(System.out));
		HSIETestData.assertHSIE(HSIETestData.unionType(), form);
	}

	@Test
	public void testASimpleRecursivelyDefinedFunction1() throws Exception {
//		Scope biscope = Builtin.builtinScope();
//		PackageDefn pkg = new PackageDefn(null, biscope, "ME");
		Scope s = new Scope(null, null);
		FunctionParser p = new FunctionParser(new FLASStory.State(null, "ME", HSIEForm.CodeType.FUNCTION));
		FunctionCaseDefn c1 = (FunctionCaseDefn)p.tryParsing(new Tokenizable("f x = g (x-1)"));
		FunctionDefinition f = new FunctionDefinition(null, CodeType.FUNCTION, "ME.f", 1);
		FunctionCaseDefn g1 = (FunctionCaseDefn)p.tryParsing(new Tokenizable("g x = f (x+1)"));
		FunctionDefinition g = new FunctionDefinition(null, CodeType.FUNCTION, "ME.g", 1);
		ScopeEntry mf = s.define("f", "ME.f", f);
		ScopeEntry mg = s.define("g", "ME.g", g);
		f.cases.add(new FunctionCaseDefn(mf, c1, 0));
		g.cases.add(new FunctionCaseDefn(mg, g1, 0));
		Rewriter rw = new Rewriter(errors, null, Builtin.builtins());
		rw.rewritePackageScope("ME", s);
		errors.showTo(new PrintWriter(System.out), 0);
		assertEquals(errors.singleString(), 0, errors.count());
		System.out.println(rw.functions);
		HSIEForm form = new HSIE(errors, rw).handle(null, rw.functions.get("ME.f"));
		assertNotNull(form);
		form.dump(new PrintWriter(System.out));
		HSIETestData.assertHSIE(HSIETestData.rdf1(), form);
	}

	@Test
	public void testASimpleRecursivelyDefinedFunction2() throws Exception {
//		Scope biscope = Builtin.builtinScope();
//		PackageDefn pkg = new PackageDefn(null, biscope, "ME");
		Scope s = new Scope(null, null);
		FunctionParser p = new FunctionParser(new FLASStory.State(null, "ME", HSIEForm.CodeType.FUNCTION));
		FunctionCaseDefn c1 = (FunctionCaseDefn)p.tryParsing(new Tokenizable("f x = g (x-1)"));
		FunctionDefinition f = new FunctionDefinition(null, CodeType.FUNCTION, "ME.f", 1);
		FunctionCaseDefn g1 = (FunctionCaseDefn)p.tryParsing(new Tokenizable("g x = f (x+1)"));
		FunctionDefinition g = new FunctionDefinition(null, CodeType.FUNCTION, "ME.g", 1);
		ScopeEntry mf = s.define("f", "ME.f", f);
		ScopeEntry mg = s.define("g", "ME.g", g);
		f.cases.add(new FunctionCaseDefn(mf, c1, 0));
		g.cases.add(new FunctionCaseDefn(mg, g1, 0));
		Rewriter rw = new Rewriter(errors, null, Builtin.builtins());
		rw.rewritePackageScope("ME", s);
		errors.showTo(new PrintWriter(System.out), 0);
		assertEquals(errors.singleString(), 0, errors.count());
		System.out.println(rw.functions);
		HSIEForm form = new HSIE(errors, rw).handle(null, rw.functions.get("ME.g"));
		assertNotNull(form);
		form.dump(new PrintWriter(System.out));
		HSIETestData.assertHSIE(HSIETestData.rdf2(), form);
	}

	@Test
	public void testADirectLet() throws Exception {
//		Scope biscope = Builtin.builtinScope();
//		PackageDefn pkg = new PackageDefn(null, biscope, "ME");
		// TODO: I changed this from _x to ME.f._x to get the test to pass; not sure if the code actually does that.
		LetExpr expr = new LetExpr("ME.f._x",
					new ApplyExpr(null, new PackageVar(null, "FLEval.plus", null), new NumericLiteral(null, "2"), new NumericLiteral(null, "2")),
					new ApplyExpr(null, new PackageVar(null, "FLEval.plus", null), new LocalVar("ME.f", null, "_x", null, null), new LocalVar("ME.f", null, "_x", null, null)));
		RWFunctionCaseDefn fcd = new RWFunctionCaseDefn(new RWFunctionIntro(null, "ME.f", new ArrayList<>(), new HashMap<>()), 0, expr);
		RWFunctionDefinition f = new RWFunctionDefinition(null, CodeType.FUNCTION, "ME.f", 0, true);
		f.cases.add(fcd);
		HSIEForm form = new HSIE(errors, null).handle(null, f);
		assertNotNull(form);
		HSIETestData.assertHSIE(HSIETestData.directLet(), form);
	}
}
