package test.flas.generator.js.env;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.TreeSet;

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
import org.flasck.flas.repository.Repository;
import org.junit.Ignore;
import org.junit.Test;
import org.zinutils.bytecode.mock.IndentWriter;
import org.zinutils.graphs.DirectedAcyclicGraph;

public class ClassGeneration {
//	private InputPosition pos = new InputPosition("-", 1, 0, null);
	private final PackageName pkg = new PackageName("test.repo");
	JSEnvironment jse = new JSEnvironment(new Repository(), new File("/tmp"), new DirectedAcyclicGraph<>(), null);
	StringWriter sw = new StringWriter();
	PrintWriter pw = new PrintWriter(sw);
	IndentWriter w = new IndentWriter(pw);
	
	@Test
	public void creatingAClassEnsuresThereIsOnePackageFile() {
		jse.newClass(new PackageName("test.repo"), new SolidName(new PackageName("test"), "Clazz"));
		List<File> acc = new ArrayList<>();
		jse.files().forEach(x -> acc.add(x));
		assertEquals(1, acc.size());
		assertEquals("/tmp/test.repo.js", acc.get(0).getPath());
	}

	@Test
	public void creatingAClassReturnsAClassObject() {
		JSClassCreator jcc = jse.newClass(new PackageName("test.repo"), new SolidName(new PackageName("test"), "Clazz"));
		assertNotNull(jcc);
		JSFile f = jse.getPackage(new PackageName("test.repo"));
		assertNotNull(f);
		assertEquals(1, f.classes().size());
	}

	@Test
	public void creatingAFunctionIsPossible() {
		JSMethodCreator meth = jse.newFunction(null, new PackageName("test.repo"), new PackageName("test.repo"), false, "f");
		assertNotNull(meth);
		JSFile f = jse.getPackage(new PackageName("test.repo"));
		assertNotNull(f);
		assertEquals(1, f.functions().size());
	}

	@Test
	public void aClassCanCreateNewMethods() {
		TreeSet<String> exports = new TreeSet<>();
		JSClass jsc = new JSClass(jse, new SolidName(new PackageName("pkg.level"), "Clz"));
		JSMethodCreator meth = jsc.createMethod("test", false);
		meth.argument("_cxt");
		assertNotNull(meth);
		meth.write(w, new HashSet<>(), exports);
		assertEquals("\npkg__level.Clz.test = function(_cxt) {\n}\n\npkg__level.Clz.test.nfargs = function() { return 0; }\n", sw.toString());
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
	public void methodsCanCallStaticFunctions() {
		w = w.indent();
		JSMethod meth = new JSMethod(jse, null, null, false, "fred");
		JSExpr expr = meth.pushFunction("test.repo.f", null, -1);
		assertNotNull(expr);
		expr.write(w);
		assertEquals("  const _v1 = test.repo.f;\n", sw.toString());
	}

	@Test
	public void methodsCanAssignMultipleVars() {
		w = w.indent();
		JSMethod meth = new JSMethod(jse, null, null, false, "fred");
		{
			JSExpr expr = meth.pushFunction("test.repo.g", null, -1);
			assertNotNull(expr);
			expr.write(w);
		}
		{
			JSExpr expr = meth.pushFunction("test.repo.f", null, -1);
			assertNotNull(expr);
			expr.write(w);
		}
		assertEquals("  const _v1 = test.repo.g;\n  const _v2 = test.repo.f;\n", sw.toString());
	}

	@Test
	public void methodsCanMakeAssertions() {
		TreeSet<String> exports = new TreeSet<>();
		w = w.indent();
		JSMethod meth = new JSMethod(jse, null, new PackageName("pkg"), false, "fred");
		meth.argument("_cxt");
		JSExpr obj = new JSVar("runner");
		meth.assertable(obj, "isSame", obj, new JSLiteral("true"));
		meth.write(w, new HashSet<>(), exports);
		assertEquals("\n  pkg.fred = function(_cxt) {\n    runner.isSame(_cxt, runner, true);\n  }\n\n  pkg.fred.nfargs = function() { return 0; }\n", sw.toString());
	}

	@Test
	public void methodsCanReturnThings() {
		TreeSet<String> exports = new TreeSet<>();
		w = w.indent();
		JSMethod fn = new JSMethod(jse, null, new PackageName("pkg"), false, "fred");
		fn.argument("_cxt");
		fn.returnObject(new JSString("hello"));
		fn.write(w, new HashSet<>(), exports);
		assertEquals("\n  pkg.fred = function(_cxt) {\n    return 'hello';\n  }\n\n  pkg.fred.nfargs = function() { return 0; }\n", sw.toString());
	}

	@Test
	public void aMethodWithOneArgumentGeneratesCorrectly() {
		TreeSet<String> exports = new TreeSet<>();
		w = w.indent();
		JSClass jsc = new JSClass(jse, new SolidName(new PackageName("pkg"), "Clz"));
		JSMethodCreator meth = jsc.createMethod("test", false);
		meth.argument("_cxt");
		assertNotNull(meth);
		meth.argument("arg1");
		meth.write(w, new HashSet<>(), exports);
		assertEquals("\n  pkg.Clz.test = function(_cxt, arg1) {\n  }\n\n  pkg.Clz.test.nfargs = function() { return 1; }\n", sw.toString());
	}

	@Test
	public void aMethodWithArgumentsGeneratesCorrectly() {
		TreeSet<String> exports = new TreeSet<>();
		w = w.indent();
		JSClass jsc = new JSClass(jse, new SolidName(new PackageName("pkg"), "Clz"));
		JSMethodCreator meth = jsc.createMethod("test", false);
		meth.argument("_cxt");
		assertNotNull(meth);
		meth.argument("arg1");
		meth.argument("arg2");
		meth.write(w, new HashSet<>(), exports);
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
		assertEquals("_v1", callG.asVar());
	}

	@Test
	public void aConstructorWithArgsGeneratesACreationExpression() {
		w = w.indent();
		JSMethodCreator meth = new JSMethod(jse, null, new PackageName("pkg"), false, "f");
		JSExpr callG = meth.pushFunction("g", null, -1);
		assertEquals("_v1", callG.asVar());
	}

	@Test
	@Ignore
	public void aPackageDefinesItsNesting() {
		JSFile f = new JSFile(null, new PackageName("test.repo.pkg"), null);
		f.writeTo(w, new ArrayList<>());
		assertEquals("if (typeof(test) === 'undefined') test = {};\nif (typeof(test.repo) === 'undefined') test.repo = {};\nif (typeof(test.repo.pkg) === 'undefined') test.repo.pkg = {};\n", sw.toString());
	}
	
	@Test
	@Ignore
	public void aPackageIncludesItsClasses() {
		JSFile f = new JSFile(null, new PackageName("test"), null);
		f.addClass(new JSClass(jse, new SolidName(new PackageName("test"), "Clazz")));
		f.writeTo(w, new ArrayList<>());
		assertEquals("if (typeof(test) === 'undefined') test = {};\n\ntest.Clazz = function() {\n}\n", sw.toString());
	}
	
	@Test
	@Ignore
	public void aPackageIncludesItsFunctions() {
		JSFile f = new JSFile(null, new PackageName("test"), null);
		JSMethod meth = new JSMethod(jse, null, new PackageName("test"), false, "f");
		meth.argument("_cxt");
		f.addFunction(meth);
		f.writeTo(w, new ArrayList<>());
		assertEquals("if (typeof(test) === 'undefined') test = {};\n\ntest.f = function(_cxt) {\n}\n\ntest.f.nfargs = function() { return 0; }\n", sw.toString());
	}
	
	@Test
	public void aClassIncludesItsMethods() {
		TreeSet<String> exports = new TreeSet<>();
		JSClass clz = new JSClass(jse, new SolidName(new PackageName("test"), "Clazz"));
		JSMethodCreator meth = clz.createMethod("f", false);
		meth.argument("_cxt");
		clz.writeTo(exports, w);
		assertEquals("\ntest.Clazz = function() {\n}\n\ntest.Clazz.f = function(_cxt) {\n}\n\ntest.Clazz.f.nfargs = function() { return 0; }\n", sw.toString());
	}
	
	@Test
	public void aClosureIsGenerated() {
		TreeSet<String> exports = new TreeSet<>();
		JSClass clz = new JSClass(jse, new SolidName(new PackageName("test"), "Clazz"));
		JSMethodCreator meth = clz.createMethod("f", false);
		meth.argument("_cxt");
		meth.closure(false, meth.pushFunction("f", null, -1), meth.string("hello"));
		clz.writeTo(exports, w);
		assertEquals("\ntest.Clazz = function() {\n}\n\ntest.Clazz.f = function(_cxt) {\n  const _v1 = f;\n  const _v2 = _cxt.closure(_v1, 'hello');\n}\n\ntest.Clazz.f.nfargs = function() { return 0; }\n", sw.toString());
	}
	
	@Test
	public void aCurryIsGenerated() {
		TreeSet<String> exports = new TreeSet<>();
		JSClass clz = new JSClass(jse, new SolidName(new PackageName("test"), "Clazz"));
		JSMethodCreator meth = clz.createMethod("f", false);
		meth.argument("_cxt");
		meth.curry(false, 2, meth.pushFunction("f", null, -1), meth.string("hello"));
		clz.writeTo(exports, w);
		assertEquals("\ntest.Clazz = function() {\n}\n\ntest.Clazz.f = function(_cxt) {\n  const _v1 = f;\n  const _v2 = _cxt.curry(2, _v1, 'hello');\n}\n\ntest.Clazz.f.nfargs = function() { return 0; }\n", sw.toString());
	}
	
	@Test
	public void anExplicitCurryIsGenerated() {
		TreeSet<String> exports = new TreeSet<>();
		JSClass clz = new JSClass(jse, new SolidName(new PackageName("test"), "Clazz"));
		JSMethodCreator meth = clz.createMethod("f", false);
		meth.argument("_cxt");
		meth.xcurry(false, 2, Arrays.asList(new XCArg(0, meth.pushFunction("f", null, -1)), new XCArg(2, meth.string("hello"))));
		clz.writeTo(exports, w);
		assertEquals("\ntest.Clazz = function() {\n}\n\ntest.Clazz.f = function(_cxt) {\n  const _v1 = f;\n  const _v2 = _cxt.xcurry(2, 0, _v1, 2, 'hello');\n}\n\ntest.Clazz.f.nfargs = function() { return 0; }\n", sw.toString());
	}
	
	@Test
	public void mockContractCallsTheRightMethod() {
		JSBlock b = new JSMethod(jse, null, null, false, "fred");
		JSExpr mc = b.mockContract(new SolidName(new PackageName("org.fred"), "Ctr"));
		assertEquals("_v1", mc.asVar());
		mc.write(new IndentWriter(new PrintWriter(sw)));
		assertEquals("const _v1 = _cxt.mockContract(new org__fred.Ctr(_cxt));\n", sw.toString());
	}
	
	@Test
	public void createObjectCallsTheRightMethod() {
		JSBlock b = new JSMethod(jse, null, null, false, "fred");
		JSExpr mc = b.createObject(new SolidName(new PackageName("org.fred"), "MyObj"));
		assertEquals("_v1", mc.asVar());
		mc.write(new IndentWriter(new PrintWriter(sw)));
		assertEquals("const _v1 = org__fred.MyObj.eval(_cxt);\n", sw.toString());
	}
	
	@Test
	public void weCanCreateNewJavascriptLevelObjects() {
		SolidName sn = new SolidName(pkg, "Obj");
		JSBlock b = new JSMethod(jse, null, null, false, "fred");
		JSExpr mc = b.newOf(sn);
		assertEquals("_v1", mc.asVar());
		mc.write(new IndentWriter(new PrintWriter(sw)));
		assertEquals("const _v1 = new test__repo.Obj(_cxt);\n", sw.toString());
	}
	
	@Test
	public void weCanCreateANewJavascriptLevelObjectAndStoreItAsAField() {
		SolidName sn = new SolidName(pkg, "Obj");
		JSBlock b = new JSMethod(jse, null, new PackageName("pkg"), false, "fred");
		JSExpr mc = b.fieldObject("state", sn);
		mc.write(new IndentWriter(new PrintWriter(sw)));
		assertEquals("this.state = new test__repo.Obj(_cxt);\n", sw.toString());
	}
	
	@Test
	public void weCanStoreValuesInTheFieldsContainer() {
		TreeSet<String> exports = new TreeSet<>();
		JSMethod b = new JSMethod(jse, null, new PackageName("pkg"), false, "fred");
		b.argument("cxt");
		b.storeField(true, null, "value", b.string("hello"));
		b.write(new IndentWriter(new PrintWriter(sw)), new HashSet<>(), exports);
		assertEquals("\npkg.fred = function(cxt) {\n  this.state.set('value', 'hello');\n}\n\npkg.fred.nfargs = function() { return 0; }\n", sw.toString());
	}
	
	@Test
	public void weCanLoadValuesFromTheFieldsContainer() {
		TreeSet<String> exports = new TreeSet<>();
		JSMethod b = new JSMethod(jse, null, new PackageName("pkg"), false, "fred");
		b.argument("cxt");
		b.returnObject(b.loadField(new JSThis(), "value"));
		b.write(new IndentWriter(new PrintWriter(sw)), new HashSet<>(), exports);
		assertEquals("\npkg.fred = function(cxt) {\n  return this.state.get('value');\n}\n\npkg.fred.nfargs = function() { return 0; }\n", sw.toString());
	}

	@Test
	public void weCanStoreValuesInAForeignFieldsContainer() {
		TreeSet<String> exports = new TreeSet<>();
		JSMethod b = new JSMethod(jse, null, new PackageName("pkg"), false, "fred");
		b.argument("cxt");
		b.storeField(true, b.boundVar("x"), "value", b.string("hello"));
		b.write(new IndentWriter(new PrintWriter(sw)), new HashSet<>(), exports);
		assertEquals("\npkg.fred = function(cxt) {\n  x.state.set('value', 'hello');\n}\n\npkg.fred.nfargs = function() { return 0; }\n", sw.toString());
	}
}
