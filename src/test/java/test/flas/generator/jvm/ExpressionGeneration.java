package test.flas.generator.jvm;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.ApplyExpr;
import org.flasck.flas.commonBase.NumericLiteral;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.compiler.JVMGenerator;
import org.flasck.flas.parsedForm.FieldsDefn.FieldsType;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.UnresolvedOperator;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.repository.Traverser;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.MethodDefiner;
import org.zinutils.bytecode.Var;


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

	// TODO: A struct constructor of no args becomes an actual eval ... (eg Nil)
	// TODO: A var with no args expecting 2 becomes a curried function ...
	
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
		IExpr x = context.mock(IExpr.class, "Ctor");
		context.checking(new Expectations() {{
			oneOf(meth).callStatic("test.repo.Ctor", "java.lang.Object", "eval"); will(returnValue(x));
		}});
		Traverser gen = new Traverser(JVMGenerator.forTests(meth, null));
		gen.visitExpr(expr, 0);
	}

	@Test
	public void anOp() {
		context.checking(new Expectations() {{
			oneOf(meth).classConst("org.flasck.jvm.fl.FLEval$Plus");
		}});
		UnresolvedOperator expr = new UnresolvedOperator(pos, "+");
		Traverser gen = new Traverser(JVMGenerator.forTests(meth, null));
		gen.visitExpr(expr, 2);
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
}
