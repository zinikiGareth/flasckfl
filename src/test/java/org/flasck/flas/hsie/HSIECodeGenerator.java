package org.flasck.flas.hsie;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.PrintWriter;
import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.errors.ErrorResult;
import org.flasck.flas.flim.Builtin;
import org.flasck.flas.parsedForm.FunctionCaseDefn;
import org.flasck.flas.parsedForm.Scope;
import org.flasck.flas.parser.FunctionParser;
import org.flasck.flas.rewriter.Rewriter;
import org.flasck.flas.rewrittenForm.FunctionName;
import org.flasck.flas.rewrittenForm.RWFunctionDefinition;
import org.flasck.flas.stories.FLASStory;
import org.flasck.flas.tokenizers.Tokenizable;
import org.flasck.flas.vcode.hsieForm.HSIEForm;
import org.flasck.flas.vcode.hsieForm.HSIEForm.CodeType;
import org.junit.Ignore;
import org.junit.Test;

// Although these are tests, they are really just to make sure that the data
// we enter in HSIETestData is valid from programs.
public class HSIECodeGenerator {
	private final InputPosition posn = new InputPosition("test", 1, 0, null);
	private ErrorResult errors = new ErrorResult();
	
	@Test
	public void testConvertingIdOf1() throws Exception {
		Scope s = new Scope(null);
		FunctionParser p = new FunctionParser(new FLASStory.State(null, "ME", HSIEForm.CodeType.FUNCTION));
		FunctionCaseDefn c1 = (FunctionCaseDefn)p.tryParsing(new Tokenizable("f = plus1 1"));
		c1.provideCaseName("ME.f_0");
		s.define("f", "ME.f", c1);
		Rewriter rw = new Rewriter(errors, null, Builtin.builtins());
		rw.functions.put("plus1", new RWFunctionDefinition(posn, CodeType.FUNCTION, FunctionName.function(null, "plus1"), 1, null, false));
		rw.rewritePackageScope(null, "ME", s);
		errors.showTo(new PrintWriter(System.out), 0);
		assertEquals(0, errors.count());
		System.out.println(rw.functions);
		HSIEForm form = HSIETestData.doHSIE(errors, rw, rw.functions.get("ME.f"));
		assertNotNull(form);
		HSIETestData.assertHSIE(HSIETestData.plus1Of1(), form);
	}

	// This is a pathological case of LET with vars
	@Test
	public void testConvertingIdDecode() throws Exception {
		Scope s = new Scope(null);
		FunctionParser p = new FunctionParser(new FLASStory.State(null, "ME", HSIEForm.CodeType.FUNCTION));
		FunctionCaseDefn c1 = (FunctionCaseDefn)p.tryParsing(new Tokenizable("f = id1 (decode (id1 32))"));
		c1.provideCaseName("ME.f_0");
		s.define("f", "ME.f", c1);
		Rewriter rw = new Rewriter(errors, null, Builtin.builtins());
		rw.functions.put("id1", new RWFunctionDefinition(posn, CodeType.FUNCTION, FunctionName.function(null, "id1"), 1, null, false));
		rw.functions.put("decode", new RWFunctionDefinition(posn, CodeType.FUNCTION, FunctionName.function(null, "decode"), 1, null, false));
		rw.rewritePackageScope(null, "ME", s);
		errors.showTo(new PrintWriter(System.out), 0);
		assertEquals(0, errors.count());
		System.out.println(rw.functions);
		HSIEForm form = HSIETestData.doHSIE(errors, rw, rw.functions.get("ME.f"));
		assertNotNull(form);
		HSIETestData.assertHSIE(HSIETestData.idDecode(), form);
	}

	@Test
	@Ignore
	public void testPatternMatchingAPolyVar() throws Exception {
		Scope s = new Scope(null);
		FunctionParser p = new FunctionParser(new FLASStory.State(null, "ME", HSIEForm.CodeType.FUNCTION));
		FunctionCaseDefn c1 = (FunctionCaseDefn)p.tryParsing(new Tokenizable("push (Cons[A] x) (A y) = Cons y x"));
		c1.provideCaseName("ME.f_0");
		s.define("push", "ME.push", c1);
		Rewriter rw = new Rewriter(errors, null, Builtin.builtins());
		rw.rewritePackageScope(null, "ME", s);
		errors.showTo(new PrintWriter(System.out), 0);
		assertEquals(errors.singleString(), 0, errors.count());
		System.out.println(rw.functions);
		HSIEForm form = HSIETestData.doHSIE(errors, rw, rw.functions.get("ME.push"));
		assertNotNull(form);
		form.dump(new PrintWriter(System.out));
		HSIETestData.assertHSIE(HSIETestData.unionType(), form);
	}

	@Test
	public void testPatternMatchingAUnionType() throws Exception {
		Scope s = new Scope(null);
		FunctionParser p = new FunctionParser(new FLASStory.State(null, "ME", HSIEForm.CodeType.FUNCTION));
		FunctionCaseDefn c1 = (FunctionCaseDefn)p.tryParsing(new Tokenizable("f (List[A] x) = 10"));
		c1.provideCaseName("ME.f_0");
		s.define("f", "ME.f", c1);
		Rewriter rw = new Rewriter(errors, null, Builtin.builtins());
		rw.rewritePackageScope(null, "ME", s);
		errors.showTo(new PrintWriter(System.out), 0);
		assertEquals(errors.singleString(), 0, errors.count());
		System.out.println(rw.functions);
		HSIEForm form = HSIETestData.doHSIE(errors, rw, rw.functions.get("ME.f"));
		assertNotNull(form);
		form.dump(new PrintWriter(System.out));
		HSIETestData.assertHSIE(HSIETestData.unionType(), form);
	}

	@Test
	public void testASimpleRecursivelyDefinedFunction1() throws Exception {
		Scope s = new Scope(null);
		FunctionParser p = new FunctionParser(new FLASStory.State(null, "ME", HSIEForm.CodeType.FUNCTION));
		FunctionCaseDefn c1 = (FunctionCaseDefn)p.tryParsing(new Tokenizable("f x = g (x-1)"));
		c1.provideCaseName("ME.f_0");
		FunctionCaseDefn g1 = (FunctionCaseDefn)p.tryParsing(new Tokenizable("g x = f (x+1)"));
		g1.provideCaseName("ME.g_0");
		s.define("f", "ME.f", c1);
		s.define("g", "ME.g", g1);
		Rewriter rw = new Rewriter(errors, null, Builtin.builtins());
		rw.rewritePackageScope(null, "ME", s);
		errors.showTo(new PrintWriter(System.out), 0);
		assertEquals(errors.singleString(), 0, errors.count());
		System.out.println(rw.functions);
		HSIEForm form = HSIETestData.doHSIE(errors, rw, rw.functions.get("ME.f"));
		assertNotNull(form);
		form.dump(new PrintWriter(System.out));
		HSIETestData.assertHSIE(HSIETestData.rdf1(), form);
	}

	@Test
	public void testASimpleRecursivelyDefinedFunction2() throws Exception {
		Scope s = new Scope(null);
		FunctionParser p = new FunctionParser(new FLASStory.State(null, "ME", HSIEForm.CodeType.FUNCTION));
		FunctionCaseDefn c1 = (FunctionCaseDefn)p.tryParsing(new Tokenizable("f x = g (x-1)"));
		c1.provideCaseName("ME.f_0");
		FunctionCaseDefn g1 = (FunctionCaseDefn)p.tryParsing(new Tokenizable("g x = f (x+1)"));
		g1.provideCaseName("ME.g_0");
		s.define("f", "ME.f", c1);
		s.define("g", "ME.g", g1);
		Rewriter rw = new Rewriter(errors, null, Builtin.builtins());
		rw.rewritePackageScope(null, "ME", s);
		errors.showTo(new PrintWriter(System.out), 0);
		assertEquals(errors.singleString(), 0, errors.count());
		System.out.println(rw.functions);
		HSIEForm form = HSIETestData.doHSIE(errors, rw, rw.functions.get("ME.g"));
		assertNotNull(form);
		form.dump(new PrintWriter(System.out));
		HSIETestData.assertHSIE(HSIETestData.rdf2(0), form);
	}
}
