package test.flas.generator.js.env;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.compiler.jsgen.JSClass;
import org.flasck.flas.compiler.jsgen.JSClassCreator;
import org.flasck.flas.compiler.jsgen.JSEnvironment;
import org.flasck.flas.compiler.jsgen.JSExpr;
import org.flasck.flas.compiler.jsgen.JSFile;
import org.flasck.flas.compiler.jsgen.JSLiteral;
import org.flasck.flas.compiler.jsgen.JSMethod;
import org.flasck.flas.compiler.jsgen.JSMethodCreator;
import org.flasck.flas.compiler.jsgen.JSString;
import org.flasck.flas.compiler.jsgen.JSVar;
import org.junit.Test;
import org.zinutils.bytecode.mock.IndentWriter;

public class ClassGeneration {
	StringWriter sw = new StringWriter();
	PrintWriter pw = new PrintWriter(sw);
	IndentWriter w = new IndentWriter(pw);
	
	@Test
	public void creatingAClassEnsuresThereIsOnePackageFile() {
		JSEnvironment jse = new JSEnvironment(new File("/tmp"));
		jse.newClass("test.repo", "Fred");
		List<File> acc = new ArrayList<>();
		jse.files().forEach(x -> acc.add(x));
		assertEquals(1, acc.size());
		assertEquals("/tmp/test.repo.js", acc.get(0).getPath());
	}

	@Test
	public void creatingAClassReturnsAClassObject() {
		JSEnvironment jse = new JSEnvironment(new File("/tmp"));
		JSClassCreator jcc = jse.newClass("test.repo", "Fred");
		assertNotNull(jcc);
	}

	@Test
	public void aClassCanCreateNewMethods() {
		w = w.indent();
		JSClass jsc = new JSClass("pkg.level", "Clz");
		JSMethodCreator meth = jsc.createMethod("test");
		assertNotNull(meth);
		meth.write(w);
		assertEquals("\n  pkg.level.Clz.test = function() {\n  }\n", sw.toString());
	}

	@Test
	public void methodsCanCreateLiterals() {
		JSMethod meth = new JSMethod(null, "fred");
		JSExpr expr = meth.literal("42");
		assertNotNull(expr);
		// I don't know if I want this or not ...
//		expr.write(w);
//		assertEquals("42", sw.toString());
		assertEquals("42", expr.asVar());
	}

	@Test
	public void methodsCanCreateStrings() {
		JSMethod meth = new JSMethod(null, "fred");
		JSExpr expr = meth.string("hello");
		assertNotNull(expr);
		// I don't know if I want this or not ...
//		expr.write(w);
//		assertEquals("42", sw.toString());
		assertEquals("'hello'", expr.asVar());
	}

	@Test
	public void methodsCanCreateArguments() {
		JSMethod meth = new JSMethod(null, "fred");
		JSExpr expr = meth.argument("v");
		assertNotNull(expr);
		expr.write(w);
		assertEquals("v", sw.toString());
		assertEquals("v", expr.asVar());
	}

	@Test
	public void methodsCanCreateApplyExprs() {
		w = w.indent();
		JSMethod meth = new JSMethod(null, "fred");
		JSExpr expr = meth.callMethod(new JSVar("v"), "called", new JSLiteral("true"));
		assertNotNull(expr);
		expr.write(w);
		assertEquals("  const v1 = v.called(true);\n", sw.toString());
	}

	@Test
	public void methodsCanCallStaticFunctions() {
		w = w.indent();
		JSMethod meth = new JSMethod(null, "fred");
		JSExpr expr = meth.callStatic("test.repo", "f", new JSLiteral("true"));
		assertNotNull(expr);
		expr.write(w);
		assertEquals("  const v1 = test.repo.f(true);\n", sw.toString());
	}

	@Test
	public void methodsCanReturnThings() {
		w = w.indent();
		JSMethod fn = new JSMethod("pkg", "fred");
		fn.returnObject(new JSString("hello"));
		fn.write(w);
		assertEquals("\n  pkg.fred = function() {\n    return 'hello';\n  }\n", sw.toString());
	}

	@Test
	public void aMethodWithOneArgumentGeneratesCorrectly() {
		w = w.indent();
		JSClass jsc = new JSClass("pkg", "Clz");
		JSMethodCreator meth = jsc.createMethod("test");
		assertNotNull(meth);
		meth.argument("arg1");
		meth.write(w);
		assertEquals("\n  pkg.Clz.test = function(arg1) {\n  }\n", sw.toString());
	}

	@Test
	public void aMethodWithArgumentsGeneratesCorrectly() {
		w = w.indent();
		JSClass jsc = new JSClass("pkg", "Clz");
		JSMethodCreator meth = jsc.createMethod("test");
		assertNotNull(meth);
		meth.argument("arg1");
		meth.argument("arg2");
		meth.write(w);
		assertEquals("\n  pkg.Clz.test = function(arg1, arg2) {\n  }\n", sw.toString());
	}

	@Test
	public void aMethodIncludesItsActions() {
		w = w.indent();
		JSClass jsc = new JSClass("pkg", "Clz");
		JSMethodCreator meth = jsc.createMethod("test");
		assertNotNull(meth);
		JSExpr obj = meth.argument("arg1");
		meth.callMethod(obj, "mymethod", obj);
		meth.write(w);
		assertEquals("\n  pkg.Clz.test = function(arg1) {\n    const v1 = arg1.mymethod(arg1);\n  }\n", sw.toString());
	}
	
	@Test
	public void aPackageDefinesItsNesting() {
		JSFile f = new JSFile("test.repo.pkg", null);
		f.writeTo(w);
		assertEquals("if (!test) test = {};\nif (!test.repo) test.repo = {};\nif (!test.repo.pkg) test.repo.pkg = {};\n", sw.toString());
	}
	
	@Test
	public void aPackageIncludesItsClasses() {
		JSFile f = new JSFile("test", null);
		f.addClass(new JSClass("test", "Clazz"));
		f.writeTo(w);
		assertEquals("if (!test) test = {};\n\ntest.Clazz = function() {\n}\n", sw.toString());
	}
	
	@Test
	public void aPackageIncludesItsFunctions() {
		JSFile f = new JSFile("test", null);
		f.addFunction(new JSMethod("test", "f"));
		f.writeTo(w);
		assertEquals("if (!test) test = {};\n\ntest.f = function() {\n}\n", sw.toString());
	}
}
