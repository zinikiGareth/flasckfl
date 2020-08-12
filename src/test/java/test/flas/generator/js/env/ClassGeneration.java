package test.flas.generator.js.env;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.commonBase.names.SolidName;
import org.flasck.flas.compiler.jsgen.JSGenerator.XCArg;
import org.flasck.flas.compiler.jsgen.creators.JSBlock;
import org.flasck.flas.compiler.jsgen.creators.JSClass;
import org.flasck.flas.compiler.jsgen.creators.JSClassCreator;
import org.flasck.flas.compiler.jsgen.creators.JSMethod;
import org.flasck.flas.compiler.jsgen.creators.JSMethodCreator;
import org.flasck.flas.compiler.jsgen.form.JSExpr;
import org.flasck.flas.compiler.jsgen.form.JSLiteral;
import org.flasck.flas.compiler.jsgen.form.JSString;
import org.flasck.flas.compiler.jsgen.form.JSThis;
import org.flasck.flas.compiler.jsgen.form.JSVar;
import org.flasck.flas.compiler.jsgen.packaging.JSEnvironment;
import org.flasck.flas.compiler.jsgen.packaging.JSFile;
import org.junit.Ignore;
import org.junit.Test;
import org.zinutils.bytecode.mock.IndentWriter;

public class ClassGeneration {
//	private InputPosition pos = new InputPosition("-", 1, 0, null);
	private final PackageName pkg = new PackageName("test.repo");
	JSEnvironment jse = new JSEnvironment(new File("/tmp"));
	StringWriter sw = new StringWriter();
	PrintWriter pw = new PrintWriter(sw);
	IndentWriter w = new IndentWriter(pw);
	
	@Test
	public void creatingAClassEnsuresThereIsOnePackageFile() {
		jse.newClass("test.repo", new SolidName(new PackageName("test"), "Clazz"));
		List<File> acc = new ArrayList<>();
		jse.files().forEach(x -> acc.add(x));
		assertEquals(1, acc.size());
		assertEquals("/tmp/test.repo.js", acc.get(0).getPath());
	}

	@Test
	public void creatingAClassReturnsAClassObject() {
		JSClassCreator jcc = jse.newClass("test.repo", new SolidName(new PackageName("test"), "Clazz"));
		assertNotNull(jcc);
		JSFile f = jse.getPackage("test.repo");
		assertNotNull(f);
		assertEquals(1, f.classes().size());
	}

	@Test
	public void creatingAFunctionIsPossible() {
		JSMethodCreator meth = jse.newFunction(null, "test.repo", new PackageName("test.repo"), false, "f");
		assertNotNull(meth);
		JSFile f = jse.getPackage("test.repo");
		assertNotNull(f);
		assertEquals(1, f.functions().size());
	}

	@Test
	public void aClassCanCreateNewMethods() {
		JSClass jsc = new JSClass(jse, new SolidName(new PackageName("pkg.level"), "Clz"));
		JSMethodCreator meth = jsc.createMethod("test", false);
		meth.argument("_cxt");
		assertNotNull(meth);
		meth.write(w);
		assertEquals("\npkg.level.Clz.test = function(_cxt) {\n}\n\npkg.level.Clz.test.nfargs = function() { return 0; }\n", sw.toString());
	}

	@Test
	public void methodsCanCreateLiterals() {
		JSMethod meth = new JSMethod(jse, null, null, false, "fred");
		JSExpr expr = meth.literal("42");
		assertNotNull(expr);
		// I don't know if I want this or not ...
//		expr.write(w);
//		assertEquals("42", sw.toString());
		assertEquals("42", expr.asVar());
	}

	@Test
	public void methodsCanCreateStrings() {
		JSMethod meth = new JSMethod(jse, null, null, false, "fred");
		JSExpr expr = meth.string("hello");
		assertNotNull(expr);
		// I don't know if I want this or not ...
//		expr.write(w);
//		assertEquals("42", sw.toString());
		assertEquals("'hello'", expr.asVar());
	}

	@Test
	public void methodsCanCreateArguments() {
		JSMethod meth = new JSMethod(jse, null, null, false, "fred");
		JSExpr expr = meth.argument("v");
		assertNotNull(expr);
		expr.write(w);
		assertEquals("v", sw.toString());
		assertEquals("v", expr.asVar());
	}

	@Test
	public void methodsCanCreateApplyExprs() {
		w = w.indent();
		JSMethod meth = new JSMethod(jse, null, null, false, "fred");
		JSExpr expr = meth.callMethod(new JSVar("v"), "called", new JSLiteral("true"));
		assertNotNull(expr);
		expr.write(w);
		assertEquals("  const v1 = v.called(_cxt, true);\n", sw.toString());
	}

	@Test
	public void methodsCanCallStaticFunctions() {
		w = w.indent();
		JSMethod meth = new JSMethod(jse, null, null, false, "fred");
		JSExpr expr = meth.pushFunction("test.repo.f", null, -1);
		assertNotNull(expr);
		expr.write(w);
		assertEquals("  const v1 = test.repo.f;\n", sw.toString());
	}

	@Test
	public void methodsCanAssignMultipleVars() {
		w = w.indent();
		JSMethod meth = new JSMethod(jse, null, null, false, "fred");
		{
			JSExpr expr = meth.callMethod(new JSVar("v"), "called", new JSLiteral("true"));
			assertNotNull(expr);
			expr.write(w);
		}
		{
			JSExpr expr = meth.pushFunction("test.repo.f", null, -1);
			assertNotNull(expr);
			expr.write(w);
		}
		assertEquals("  const v1 = v.called(_cxt, true);\n  const v2 = test.repo.f;\n", sw.toString());
	}

	@Test
	public void methodsCanMakeAssertions() {
		w = w.indent();
		JSMethod meth = new JSMethod(jse, null, new PackageName("pkg"), false, "fred");
		meth.argument("_cxt");
		JSExpr obj = new JSVar("runner");
		meth.assertable(obj, "isSame", obj, new JSLiteral("true"));
		meth.write(w);
		assertEquals("\n  pkg.fred = function(_cxt) {\n    runner.isSame(_cxt, runner, true);\n  }\n\n  pkg.fred.nfargs = function() { return 0; }\n", sw.toString());
	}

	@Test
	public void methodsCanReturnThings() {
		w = w.indent();
		JSMethod fn = new JSMethod(jse, null, new PackageName("pkg"), false, "fred");
		fn.argument("_cxt");
		fn.returnObject(new JSString("hello"));
		fn.write(w);
		assertEquals("\n  pkg.fred = function(_cxt) {\n    return 'hello';\n  }\n\n  pkg.fred.nfargs = function() { return 0; }\n", sw.toString());
	}

	@Test
	public void aMethodWithOneArgumentGeneratesCorrectly() {
		w = w.indent();
		JSClass jsc = new JSClass(jse, new SolidName(new PackageName("pkg"), "Clz"));
		JSMethodCreator meth = jsc.createMethod("test", false);
		meth.argument("_cxt");
		assertNotNull(meth);
		meth.argument("arg1");
		meth.write(w);
		assertEquals("\n  pkg.Clz.test = function(_cxt, arg1) {\n  }\n\n  pkg.Clz.test.nfargs = function() { return 1; }\n", sw.toString());
	}

	@Test
	public void aMethodWithArgumentsGeneratesCorrectly() {
		w = w.indent();
		JSClass jsc = new JSClass(jse, new SolidName(new PackageName("pkg"), "Clz"));
		JSMethodCreator meth = jsc.createMethod("test", false);
		meth.argument("_cxt");
		assertNotNull(meth);
		meth.argument("arg1");
		meth.argument("arg2");
		meth.write(w);
		assertEquals("\n  pkg.Clz.test = function(_cxt, arg1, arg2) {\n  }\n\n  pkg.Clz.test.nfargs = function() { return 2; }\n", sw.toString());
	}

	
	// TODO: I think I've started to lose the plot here, and I think in part it's
	// because I don't have golden tests keeping me honest.
	
	// Among the cases are (I believe):
	//  "just" pushing a symbol, e.g. "f", in which case it writes nothing and returns "f" asVar()
	//  evaluating a function with no args - writes closure(f) and returns v1
	//  evaluating a function with args - writes v3 = closure(f, v1, v2) and returns v3
	//  evaluating a Constructor with no args - writes nothing and returns Nil()
	//  evaluating a Constructor with args - writes v1 = Cons(v2, v3) and returns v1
	@Test
	@Ignore
	public void aFunctionCallDefinesAVar() {
		w = w.indent();
		JSMethodCreator meth = new JSMethod(jse, null, new PackageName("pkg"), false, "f");
		JSExpr callG = meth.pushFunction("g", null, -1);
		callG.write(w);
		assertEquals("const v1 = FLEval.closure(g);", sw.toString());
		assertEquals("v1", callG.asVar());
	}

	@Test
	public void aConstructorWithNoArgsGeneratesAConstant() {
		w = w.indent();
		JSMethodCreator meth = new JSMethod(jse, null, new PackageName("pkg"), false, "f");
		JSExpr callG = meth.pushFunction("g", null, -1);
		assertEquals("v1", callG.asVar());
	}

	@Test
	public void aConstructorWithArgsGeneratesACreationExpression() {
		w = w.indent();
		JSMethodCreator meth = new JSMethod(jse, null, new PackageName("pkg"), false, "f");
		JSExpr callG = meth.pushFunction("g", null, -1);
		assertEquals("v1", callG.asVar());
	}

	@Test
	public void aMethodIncludesItsActions() {
		w = w.indent();
		JSClass jsc = new JSClass(jse, new SolidName(new PackageName("pkg"), "Clz"));
		JSMethodCreator meth = jsc.createMethod("test", false);
		meth.argument("_cxt");
		assertNotNull(meth);
		JSExpr obj = meth.argument("arg1");
		meth.callMethod(obj, "mymethod", obj);
		meth.write(w);
		assertEquals("\n  pkg.Clz.test = function(_cxt, arg1) {\n    const v1 = arg1.mymethod(_cxt, arg1);\n  }\n\n  pkg.Clz.test.nfargs = function() { return 1; }\n", sw.toString());
	}
	
	@Test
	public void aPackageDefinesItsNesting() {
		JSFile f = new JSFile("test.repo.pkg", null);
		f.writeTo(w);
		assertEquals("if (typeof(test) === 'undefined') test = {};\nif (typeof(test.repo) === 'undefined') test.repo = {};\nif (typeof(test.repo.pkg) === 'undefined') test.repo.pkg = {};\n", sw.toString());
	}
	
	@Test
	public void aPackageIncludesItsClasses() {
		JSFile f = new JSFile("test", null);
		f.addClass(new JSClass(jse, new SolidName(new PackageName("test"), "Clazz")));
		f.writeTo(w);
		assertEquals("if (typeof(test) === 'undefined') test = {};\n\ntest.Clazz = function() {\n}\n", sw.toString());
	}
	
	@Test
	public void aPackageIncludesItsFunctions() {
		JSFile f = new JSFile("test", null);
		JSMethod meth = new JSMethod(jse, null, new PackageName("test"), false, "f");
		meth.argument("_cxt");
		f.addFunction(meth);
		f.writeTo(w);
		assertEquals("if (typeof(test) === 'undefined') test = {};\n\ntest.f = function(_cxt) {\n}\n\ntest.f.nfargs = function() { return 0; }\n", sw.toString());
	}
	
	@Test
	public void aClassIncludesItsMethods() {
		JSClass clz = new JSClass(jse, new SolidName(new PackageName("test"), "Clazz"));
		JSMethodCreator meth = clz.createMethod("f", false);
		meth.argument("_cxt");
		clz.writeTo(w);
		assertEquals("\ntest.Clazz = function() {\n}\n\ntest.Clazz.f = function(_cxt) {\n}\n\ntest.Clazz.f.nfargs = function() { return 0; }\n", sw.toString());
	}
	
	@Test
	public void aClosureIsGenerated() {
		JSClass clz = new JSClass(jse, new SolidName(new PackageName("test"), "Clazz"));
		JSMethodCreator meth = clz.createMethod("f", false);
		meth.argument("_cxt");
		meth.closure(false, meth.pushFunction("f", null, -1), meth.string("hello"));
		clz.writeTo(w);
		assertEquals("\ntest.Clazz = function() {\n}\n\ntest.Clazz.f = function(_cxt) {\n  const v1 = f;\n  const v2 = _cxt.closure(v1, 'hello');\n}\n\ntest.Clazz.f.nfargs = function() { return 0; }\n", sw.toString());
	}
	
	@Test
	public void aCurryIsGenerated() {
		JSClass clz = new JSClass(jse, new SolidName(new PackageName("test"), "Clazz"));
		JSMethodCreator meth = clz.createMethod("f", false);
		meth.argument("_cxt");
		meth.curry(false, 2, meth.pushFunction("f", null, -1), meth.string("hello"));
		clz.writeTo(w);
		assertEquals("\ntest.Clazz = function() {\n}\n\ntest.Clazz.f = function(_cxt) {\n  const v1 = f;\n  const v2 = _cxt.curry(2, v1, 'hello');\n}\n\ntest.Clazz.f.nfargs = function() { return 0; }\n", sw.toString());
	}
	
	@Test
	public void anExplicitCurryIsGenerated() {
		JSClass clz = new JSClass(jse, new SolidName(new PackageName("test"), "Clazz"));
		JSMethodCreator meth = clz.createMethod("f", false);
		meth.argument("_cxt");
		meth.xcurry(false, 2, Arrays.asList(new XCArg(0, meth.pushFunction("f", null, -1)), new XCArg(2, meth.string("hello"))));
		clz.writeTo(w);
		assertEquals("\ntest.Clazz = function() {\n}\n\ntest.Clazz.f = function(_cxt) {\n  const v1 = f;\n  const v2 = _cxt.xcurry(2, 0, v1, 2, 'hello');\n}\n\ntest.Clazz.f.nfargs = function() { return 0; }\n", sw.toString());
	}
	
	@Test
	public void mockContractCallsTheRightMethod() {
		JSBlock b = new JSMethod(jse, null, null, false, "fred");
		JSExpr mc = b.mockContract(new SolidName(new PackageName("org.fred"), "Ctr"));
		assertEquals("v1", mc.asVar());
		mc.write(new IndentWriter(new PrintWriter(sw)));
		assertEquals("const v1 = _cxt.mockContract(new org.fred.Ctr(_cxt));\n", sw.toString());
	}
	
	@Test
	public void createObjectCallsTheRightMethod() {
		JSBlock b = new JSMethod(jse, null, null, false, "fred");
		JSExpr mc = b.createObject(new SolidName(new PackageName("org.fred"), "MyObj"));
		assertEquals("v1", mc.asVar());
		mc.write(new IndentWriter(new PrintWriter(sw)));
		assertEquals("const v1 = org.fred.MyObj.eval(_cxt);\n", sw.toString());
	}
	
	@Test
	public void weCanCreateNewJavascriptLevelObjects() {
		SolidName sn = new SolidName(pkg, "Obj");
		JSBlock b = new JSMethod(jse, null, null, false, "fred");
		JSExpr mc = b.newOf(sn);
		assertEquals("v1", mc.asVar());
		mc.write(new IndentWriter(new PrintWriter(sw)));
		assertEquals("const v1 = new test.repo.Obj(_cxt);\n", sw.toString());
	}
	
	@Test
	public void weCanCreateANewJavascriptLevelObjectAndStoreItAsAField() {
		SolidName sn = new SolidName(pkg, "Obj");
		JSBlock b = new JSMethod(jse, null, new PackageName("pkg"), false, "fred");
		JSExpr mc = b.fieldObject("state", sn);
		mc.write(new IndentWriter(new PrintWriter(sw)));
		assertEquals("this.state = new test.repo.Obj(_cxt);\n", sw.toString());
	}
	
	@Test
	public void weCanStoreValuesInTheFieldsContainer() {
		JSMethod b = new JSMethod(jse, null, new PackageName("pkg"), false, "fred");
		b.argument("cxt");
		b.storeField(true, null, "value", b.string("hello"));
		b.write(new IndentWriter(new PrintWriter(sw)));
		assertEquals("\npkg.fred = function(cxt) {\n  this.state.set('value', 'hello');\n}\n\npkg.fred.nfargs = function() { return 0; }\n", sw.toString());
	}
	
	@Test
	public void weCanLoadValuesFromTheFieldsContainer() {
		JSMethod b = new JSMethod(jse, null, new PackageName("pkg"), false, "fred");
		b.argument("cxt");
		b.returnObject(b.loadField(new JSThis(), "value"));
		b.write(new IndentWriter(new PrintWriter(sw)));
		assertEquals("\npkg.fred = function(cxt) {\n  return this.state.get('value');\n}\n\npkg.fred.nfargs = function() { return 0; }\n", sw.toString());
	}

	@Test
	public void weCanStoreValuesInAForeignFieldsContainer() {
		JSMethod b = new JSMethod(jse, null, new PackageName("pkg"), false, "fred");
		b.argument("cxt");
		b.storeField(true, b.boundVar("x"), "value", b.string("hello"));
		b.write(new IndentWriter(new PrintWriter(sw)));
		assertEquals("\npkg.fred = function(cxt) {\n  x.state.set('value', 'hello');\n}\n\npkg.fred.nfargs = function() { return 0; }\n", sw.toString());
	}
}
