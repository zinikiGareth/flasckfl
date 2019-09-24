package test.flas.generator.jvm;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.ApplyExpr;
import org.flasck.flas.commonBase.NumericLiteral;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.commonBase.names.VarName;
import org.flasck.flas.compiler.JVMGenerator;
import org.flasck.flas.parsedForm.FieldsDefn.FieldsType;
import org.flasck.flas.parsedForm.FunctionCaseDefn;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.StructField;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.parsedForm.UnresolvedOperator;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.parsedForm.VarPattern;
import org.flasck.flas.patterns.HSIPatternTree;
import org.flasck.flas.repository.Traverser;
import org.flasck.jvm.J;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.zinutils.bytecode.ByteCodeSink;
import org.zinutils.bytecode.ByteCodeStorage;
import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.MethodDefiner;
import org.zinutils.bytecode.Var;
import org.zinutils.bytecode.mock.VarMatcher;

public class ExpressionGeneration {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	private InputPosition pos = new InputPosition("-", 1, 0, null);
	private final PackageName pkg = new PackageName("test.repo");
	private final MethodDefiner meth = context.mock(MethodDefiner.class);

	@Before
	public void setup() {
		context.checking(new Expectations() {{
			allowing(meth).lenientMode(with(any(Boolean.class)));
		}});
	}
	
	@Test
	public void aSimpleInteger() {
		NumericLiteral expr = new NumericLiteral(pos, 42);
		IExpr dv = context.mock(IExpr.class, "dv");
		IExpr iv = context.mock(IExpr.class, "iv");
		IExpr biv = context.mock(IExpr.class, "biv");
		IExpr cdv = context.mock(IExpr.class, "cdv");
		context.checking(new Expectations() {{
			oneOf(meth).aNull(); will(returnValue(dv));
			oneOf(meth).intConst(42); will(returnValue(iv));
			oneOf(meth).box(iv); will(returnValue(biv));
			oneOf(meth).castTo(dv, "java.lang.Double"); will(returnValue(cdv));
			oneOf(meth).makeNew("org.flasck.jvm.builtin.FLNumber", biv, cdv);
		}});
		Traverser gen = new Traverser(JVMGenerator.forTests(meth, null));
		gen.visitExpr(expr, 0);
	}

	@Test
	public void aSimpleString() {
		StringLiteral expr = new StringLiteral(pos, "hello");
		context.checking(new Expectations() {{
			oneOf(meth).stringConst("hello");
		}});
		Traverser gen = new Traverser(JVMGenerator.forTests(meth, null));
		gen.visitExpr(expr, 0);
	}

	@Test
	public void aVar() {
		UnresolvedVar expr = new UnresolvedVar(pos, "x");
		FunctionName nameX = FunctionName.function(pos, pkg, "x");
		expr.bind(new FunctionDefinition(nameX, 0));
		context.checking(new Expectations() {{
			oneOf(meth).classConst("test.repo.PACKAGEFUNCTIONS$x");
		}});
		Traverser gen = new Traverser(JVMGenerator.forTests(meth, null));
		gen.visitExpr(expr, 2);
	}

	@Test
	public void aFunctionArgument() {
		UnresolvedVar expr = new UnresolvedVar(pos, "p");
		FunctionName nameX = FunctionName.function(pos, pkg, "p");
		expr.bind(new VarPattern(pos, new VarName(pos, nameX, "p")));
		Var ax = null;
		IExpr cx = context.mock(IExpr.class, "cx");
		IExpr args = context.mock(IExpr.class, "args");
		IExpr head0 = context.mock(IExpr.class, "head0");
		context.checking(new Expectations() {{
			oneOf(meth).arrayItem(J.OBJECT, ax, 0); will(returnValue(args));
			oneOf(meth).nextLocal(); will(returnValue(18));
			oneOf(meth).callStatic(J.FLEVAL, J.OBJECT, "head", cx, args); will(returnValue(head0));
			oneOf(meth).assign(with(VarMatcher.local(18)), with(head0));
		}});
		Traverser gen = new Traverser(JVMGenerator.forTests(meth, cx));
		gen.visitExpr(expr, 2);
	}

	@Test
	public void aVarWithNoArgsExpectingNoArgsBecomesAClosureByItself() {
		UnresolvedVar expr = new UnresolvedVar(pos, "x");
		FunctionName nameX = FunctionName.function(pos, pkg, "x");
		expr.bind(new FunctionDefinition(nameX, 0));
		IExpr x = context.mock(IExpr.class, "x");
		IExpr xAsObj = context.mock(IExpr.class, "xAsObj");
		IExpr aev = context.mock(IExpr.class, "aev");
		IExpr args = context.mock(IExpr.class, "args");
		List<IExpr> argsList = new ArrayList<>();
		context.checking(new Expectations() {{
			oneOf(meth).nextLocal(); will(returnValue(22));
		}});
		Var var = new Var.AVar(meth, "org.flasck.jvm.fl.FLClosure", "v1");
		IExpr assign = context.mock(IExpr.class, "assign");
		context.checking(new Expectations() {{
			oneOf(meth).classConst("test.repo.PACKAGEFUNCTIONS$x"); will(returnValue(x));
			oneOf(meth).arrayOf("java.lang.Object", argsList); will(returnValue(args));
			oneOf(meth).as(x, "java.lang.Object"); will(returnValue(xAsObj));
			oneOf(meth).callStatic("org.flasck.jvm.fl.FLClosure", "org.flasck.jvm.fl.FLClosure", "simple", xAsObj, args); will(returnValue(aev));
			oneOf(meth).avar("org.flasck.jvm.fl.FLClosure", "v1"); will(returnValue(var));
			oneOf(meth).assign(with(any(Var.class)), with(aev)); will(returnValue(assign));
			oneOf(assign).flush();

		}});
		Traverser gen = new Traverser(JVMGenerator.forTests(meth, null));
		gen.visitExpr(expr, 0);
	}

	@Test
	public void aVarWithNoArgsExpectingTwoArgsBecomesACurriedFunction() {
		UnresolvedVar expr = new UnresolvedVar(pos, "x");
		FunctionName nameX = FunctionName.function(pos, pkg, "x");
		expr.bind(new FunctionDefinition(nameX, 2));
		IExpr x = context.mock(IExpr.class, "x");
		IExpr xAsObj = context.mock(IExpr.class, "xAsObj");
		IExpr expArgs = context.mock(IExpr.class, "expArgs");
		IExpr aev = context.mock(IExpr.class, "aev");
		IExpr args = context.mock(IExpr.class, "args");
		List<IExpr> argsList = new ArrayList<>();
		context.checking(new Expectations() {{
			oneOf(meth).nextLocal(); will(returnValue(22));
		}});
		Var var = new Var.AVar(meth, "org.flasck.jvm.fl.FLClosure", "v1");
		IExpr assign = context.mock(IExpr.class, "assign");
		context.checking(new Expectations() {{
			oneOf(meth).classConst("test.repo.PACKAGEFUNCTIONS$x"); will(returnValue(x));
			oneOf(meth).intConst(2); will(returnValue(expArgs));
			oneOf(meth).arrayOf("java.lang.Object", argsList); will(returnValue(args));
			oneOf(meth).as(x, "java.lang.Object"); will(returnValue(xAsObj));
			oneOf(meth).callStatic("org.flasck.jvm.fl.FLClosure", "org.flasck.jvm.fl.FLClosure", "curry", xAsObj, expArgs, args); will(returnValue(aev));
			oneOf(meth).avar("org.flasck.jvm.fl.FLClosure", "v1"); will(returnValue(var));
			oneOf(meth).assign(with(any(Var.class)), with(aev)); will(returnValue(assign));
			oneOf(assign).flush();
		}});
		Traverser gen = new Traverser(JVMGenerator.forTests(meth, null));
		gen.visitExpr(expr, 0);
	}

	@Test
	public void aStructConstructorWithNoArgsExpectingNoArgsBecomesAConstant() {
		UnresolvedVar expr = new UnresolvedVar(pos, "Ctor");
		expr.bind(new StructDefn(pos, FieldsType.STRUCT, "test.repo", "Ctor", true));
		IExpr fcx = context.mock(IExpr.class, "fcx");
		List<IExpr> argsList = new ArrayList<>();
		IExpr arr = context.mock(IExpr.class, "arr");
		context.checking(new Expectations() {{
			oneOf(meth).arrayOf("java.lang.Object", argsList); will(returnValue(arr));
			oneOf(meth).callStatic("test.repo.Ctor", "java.lang.Object", "eval", fcx, arr);
		}});
		Traverser gen = new Traverser(JVMGenerator.forTests(meth, fcx));
		gen.visitExpr(expr, 0);
	}

	@Test
	public void aFunctionRecognizesAStructConstructorWithNoArgsAndGeneratesTheStaticCall() {
		ByteCodeStorage bce = context.mock(ByteCodeStorage.class);
		ByteCodeSink bcc = context.mock(ByteCodeSink.class);
		FunctionName name = FunctionName.function(pos, pkg, "x");
		FunctionDefinition fn = new FunctionDefinition(name, 0);
		FunctionIntro fi = new FunctionIntro(name, new ArrayList<>());
		UnresolvedVar expr = new UnresolvedVar(pos, "Ctor");
		expr.bind(new StructDefn(pos, FieldsType.STRUCT, "test.repo", "Ctor", true));
		FunctionCaseDefn fcd = new FunctionCaseDefn(null, expr);
		fi.functionCase(fcd);
		fn.intro(fi);
		HSIPatternTree hsi = new HSIPatternTree(0);
		hsi.consider(fi);
		fn.bindHsi(hsi);
		List<IExpr> argsList = new ArrayList<>();
		IExpr arr = context.mock(IExpr.class, "arr");
		IExpr x = context.mock(IExpr.class, "Ctor");
		context.checking(new Expectations() {{
			oneOf(meth).nextLocal(); will(returnValue(22));
			oneOf(meth).nextLocal(); will(returnValue(23));
		}});
		Var cxt = new Var.AVar(meth, "org.ziniki.ziwsh.json.FLEvalContext", "cxt");
		Var args = new Var.AVar(meth, "JVMRunner", "runner");
		context.checking(new Expectations() {{
			oneOf(bce).newClass("test.repo.PACKAGEFUNCTIONS$x"); will(returnValue(bcc));
			oneOf(bcc).createMethod(true, "java.lang.Object", "eval"); will(returnValue(meth));
			oneOf(meth).argument("org.ziniki.ziwsh.json.FLEvalContext", "cxt"); will(returnValue(cxt));
			oneOf(meth).argument("[java.lang.Object", "args"); will(returnValue(args));
			oneOf(meth).arrayOf("java.lang.Object", argsList); will(returnValue(arr));
			oneOf(meth).callStatic("test.repo.Ctor", "java.lang.Object", "eval", cxt, arr); will(returnValue(x));
			oneOf(meth).returnObject(x);
		}});
		Traverser gen = new Traverser(new JVMGenerator(bce));
		gen.visitFunction(fn);
	}

	@Test
	public void anOp() {
		context.checking(new Expectations() {{
			oneOf(meth).classConst("org.flasck.jvm.fl.FLEval$Plus");
		}});
		UnresolvedOperator expr = new UnresolvedOperator(pos, "+");
		expr.bind(new FunctionDefinition(FunctionName.function(pos, null, "+"), 2));
		Traverser gen = new Traverser(JVMGenerator.forTests(meth, null));
		gen.visitExpr(expr, 2);
	}

	@Test
	public void anOpCanBeAStructDefn() {
		ByteCodeStorage bce = context.mock(ByteCodeStorage.class);
		ByteCodeSink bcc = context.mock(ByteCodeSink.class);
		FunctionName name = FunctionName.function(pos, pkg, "x");
		FunctionDefinition fn = new FunctionDefinition(name, 0);
		FunctionIntro fi = new FunctionIntro(name, new ArrayList<>());
		UnresolvedOperator expr = new UnresolvedOperator(pos, "[]");
		expr.bind(new StructDefn(pos, FieldsType.STRUCT, null, "Nil", true));
		FunctionCaseDefn fcd = new FunctionCaseDefn(null, expr);
		fi.functionCase(fcd);
		fn.intro(fi);
		HSIPatternTree hsi = new HSIPatternTree(0);
		hsi.consider(fi);
		fn.bindHsi(hsi);
		List<IExpr> argsList = new ArrayList<>();
		IExpr arr = context.mock(IExpr.class, "arr");
		IExpr x = context.mock(IExpr.class, "Ctor");
		context.checking(new Expectations() {{
			oneOf(meth).nextLocal(); will(returnValue(22));
			oneOf(meth).nextLocal(); will(returnValue(23));
		}});
		Var cxt = new Var.AVar(meth, "org.ziniki.ziwsh.json.FLEvalContext", "cxt");
		Var args = new Var.AVar(meth, "JVMRunner", "runner");
		context.checking(new Expectations() {{
			oneOf(bce).newClass("test.repo.PACKAGEFUNCTIONS$x"); will(returnValue(bcc));
			oneOf(bcc).createMethod(true, "java.lang.Object", "eval"); will(returnValue(meth));
			oneOf(meth).argument("org.ziniki.ziwsh.json.FLEvalContext", "cxt"); will(returnValue(cxt));
			oneOf(meth).argument("[java.lang.Object", "args"); will(returnValue(args));
			oneOf(meth).arrayOf("java.lang.Object", argsList); will(returnValue(arr));
			oneOf(meth).callStatic("org.flasck.jvm.builtin.Nil", "java.lang.Object", "eval", cxt, arr); will(returnValue(x));
			oneOf(meth).returnObject(x);
		}});
		Traverser gen = new Traverser(new JVMGenerator(bce));
		gen.visitFunction(fn);
	}

	@Test
	public void aFunctionApplication() {
		UnresolvedVar fn = new UnresolvedVar(pos, "f");
		FunctionName fnName = FunctionName.function(pos, pkg, "f");
		fn.bind(new FunctionDefinition(fnName, 2));
		ApplyExpr ae = new ApplyExpr(pos, fn, new NumericLiteral(pos, "42", 2), new StringLiteral(pos, "hello"));
		IExpr f = context.mock(IExpr.class, "f");
		IExpr fAsObj = context.mock(IExpr.class, "fAsObj");
		IExpr dv = context.mock(IExpr.class, "dv");
		IExpr iv = context.mock(IExpr.class, "iv");
		IExpr biv = context.mock(IExpr.class, "biv");
		IExpr cdv = context.mock(IExpr.class, "cdv");
		IExpr nv = context.mock(IExpr.class, "nv");
		IExpr sv = context.mock(IExpr.class, "sv");
		IExpr aev = context.mock(IExpr.class, "aev");
		IExpr args = context.mock(IExpr.class, "args");
		List<IExpr> argsList = new ArrayList<>();
		argsList.add(nv);
		argsList.add(sv);
		context.checking(new Expectations() {{
			oneOf(meth).nextLocal(); will(returnValue(22));
		}});
		Var var = new Var.AVar(meth, "org.flasck.jvm.fl.FLClosure", "v1");
		IExpr assign = context.mock(IExpr.class, "assign");
		context.checking(new Expectations() {{
			oneOf(meth).classConst("test.repo.PACKAGEFUNCTIONS$f"); will(returnValue(f));
			oneOf(meth).aNull(); will(returnValue(dv));
			oneOf(meth).intConst(42); will(returnValue(iv));
			oneOf(meth).box(iv); will(returnValue(biv));
			oneOf(meth).castTo(dv, "java.lang.Double"); will(returnValue(cdv));
			oneOf(meth).makeNew("org.flasck.jvm.builtin.FLNumber", biv, cdv); will(returnValue(nv));
			oneOf(meth).stringConst("hello"); will(returnValue(sv));
			oneOf(meth).arrayOf("java.lang.Object", argsList); will(returnValue(args));
			oneOf(meth).as(f, "java.lang.Object"); will(returnValue(fAsObj));
			oneOf(meth).callStatic("org.flasck.jvm.fl.FLClosure", "org.flasck.jvm.fl.FLClosure", "simple", fAsObj, args); will(returnValue(aev));
			oneOf(meth).avar("org.flasck.jvm.fl.FLClosure", "v1"); will(returnValue(var));
			oneOf(meth).assign(with(any(Var.class)), with(aev)); will(returnValue(assign));
			oneOf(assign).flush();
		}});
		Traverser gen = new Traverser(JVMGenerator.forTests(meth, null));
		gen.visitExpr(ae, 0);
	}

	@Test
	public void aNestedFunctionApplication() {
		UnresolvedVar fn = new UnresolvedVar(pos, "f");
		FunctionName fnName = FunctionName.function(pos, pkg, "f");
		fn.bind(new FunctionDefinition(fnName, 1));
		UnresolvedVar var = new UnresolvedVar(pos, "x");
		FunctionName varName = FunctionName.function(pos, pkg, "x");
		var.bind(new FunctionDefinition(varName, 0));
		ApplyExpr ae = new ApplyExpr(pos, fn, var);
		IExpr f = context.mock(IExpr.class, "f");
		IExpr fAsObj = context.mock(IExpr.class, "fAsObj");
		IExpr x = context.mock(IExpr.class, "x");
		IExpr xAsObj = context.mock(IExpr.class, "xAsObj");
		List<IExpr> xArgsList  = new ArrayList<>();
		IExpr xargs = context.mock(IExpr.class, "xargs");
		IExpr xae = context.mock(IExpr.class, "xae");
		IExpr aev = context.mock(IExpr.class, "aev");
		context.checking(new Expectations() {{
			oneOf(meth).nextLocal(); will(returnValue(22));
			oneOf(meth).nextLocal(); will(returnValue(23));
		}});
		Var v1 = new Var.AVar(meth, "org.flasck.jvm.fl.FLClosure", "v1");
		Var v2 = new Var.AVar(meth, "org.flasck.jvm.fl.FLClosure", "v2");
		IExpr args = context.mock(IExpr.class, "args");
		List<IExpr> argsList = new ArrayList<>();
		argsList.add(v1);
		IExpr assignx = context.mock(IExpr.class, "assignx");
		IExpr assignae = context.mock(IExpr.class, "assignae");
		context.checking(new Expectations() {{
			oneOf(meth).classConst("test.repo.PACKAGEFUNCTIONS$f"); will(returnValue(f));
			oneOf(meth).classConst("test.repo.PACKAGEFUNCTIONS$x"); will(returnValue(x));
			oneOf(meth).as(x, "java.lang.Object"); will(returnValue(xAsObj));
			oneOf(meth).arrayOf("java.lang.Object", xArgsList); will(returnValue(xargs));
			oneOf(meth).callStatic("org.flasck.jvm.fl.FLClosure", "org.flasck.jvm.fl.FLClosure", "simple", xAsObj, xargs); will(returnValue(xae));
			oneOf(meth).avar("org.flasck.jvm.fl.FLClosure", "v1"); will(returnValue(v1));
			oneOf(meth).assign(with(any(Var.class)), with(xae)); will(returnValue(assignx));
			oneOf(assignx).flush();

			oneOf(meth).arrayOf("java.lang.Object", argsList); will(returnValue(args));
			oneOf(meth).as(f, "java.lang.Object"); will(returnValue(fAsObj));
			oneOf(meth).callStatic("org.flasck.jvm.fl.FLClosure", "org.flasck.jvm.fl.FLClosure", "simple", fAsObj, args); will(returnValue(aev));
			oneOf(meth).avar("org.flasck.jvm.fl.FLClosure", "v2"); will(returnValue(v2));
			oneOf(meth).assign(with(any(Var.class)), with(aev)); will(returnValue(assignae));
			oneOf(assignae).flush();
		}});
		Traverser gen = new Traverser(JVMGenerator.forTests(meth, null));
		gen.visitExpr(ae, 0);
	}

	@Test
	public void aConstructorApplicationWithArgs() {
		UnresolvedVar nilOp = new UnresolvedVar(pos, "Nil");
		UnresolvedVar fn = new UnresolvedVar(pos, "Cons");
		StructDefn nilT = new StructDefn(pos, FieldsType.STRUCT, null, "Nil", false);
		StructDefn consT = new StructDefn(pos, FieldsType.STRUCT, null, "Cons", false);
		consT.addField(new StructField(pos, false, new TypeReference(pos, "A"), "head"));
		consT.addField(new StructField(pos, false, new TypeReference(pos, "List", new TypeReference(pos, "A")), "tail"));
		fn.bind(consT);
		nilOp.bind(nilT);
		IExpr fcx = context.mock(IExpr.class, "fcx");
		ApplyExpr ae = new ApplyExpr(pos, fn, new StringLiteral(pos, "hello"), nilOp);
		IExpr shello = context.mock(IExpr.class, "shello");
		List<IExpr> emptyList = new ArrayList<>();
		IExpr nilArgs = context.mock(IExpr.class, "nilArgs");
		IExpr nil = context.mock(IExpr.class, "nil");
		List<IExpr> argsList = new ArrayList<>();
		argsList.add(shello);
		argsList.add(nil);
		IExpr args = context.mock(IExpr.class, "args");
		context.checking(new Expectations() {{
			oneOf(meth).stringConst("hello"); will(returnValue(shello));
			oneOf(meth).arrayOf("java.lang.Object", emptyList); will(returnValue(nilArgs));
			oneOf(meth).callStatic("org.flasck.jvm.builtin.Nil", "java.lang.Object", "eval", fcx, nilArgs); will(returnValue(nil));
			oneOf(meth).arrayOf("java.lang.Object", argsList); will(returnValue(args));
			oneOf(meth).callStatic("org.flasck.jvm.builtin.Cons", "java.lang.Object", "eval", fcx, args); will(returnValue(nil));
		}});
		Traverser gen = new Traverser(JVMGenerator.forTests(meth, fcx));
		gen.visitExpr(ae, 0);
	}

	@Test
	public void aSpuriousApplyExprIsIgnored() {
		NumericLiteral nl = new NumericLiteral(pos, 42);
		ApplyExpr expr = new ApplyExpr(pos, nl);
		IExpr dv = context.mock(IExpr.class, "dv");
		IExpr iv = context.mock(IExpr.class, "iv");
		IExpr biv = context.mock(IExpr.class, "biv");
		IExpr cdv = context.mock(IExpr.class, "cdv");
		context.checking(new Expectations() {{
			oneOf(meth).aNull(); will(returnValue(dv));
			oneOf(meth).intConst(42); will(returnValue(iv));
			oneOf(meth).box(iv); will(returnValue(biv));
			oneOf(meth).castTo(dv, "java.lang.Double"); will(returnValue(cdv));
			oneOf(meth).makeNew("org.flasck.jvm.builtin.FLNumber", biv, cdv);
		}});
		Traverser gen = new Traverser(JVMGenerator.forTests(meth, null));
		gen.visitExpr(expr, 0);
	}

}
