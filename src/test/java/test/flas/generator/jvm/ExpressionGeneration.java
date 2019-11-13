package test.flas.generator.jvm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.ApplyExpr;
import org.flasck.flas.commonBase.NumericLiteral;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.commonBase.names.SolidName;
import org.flasck.flas.commonBase.names.VarName;
import org.flasck.flas.compiler.jvmgen.ApplyExprGenerator;
import org.flasck.flas.compiler.jvmgen.ExprGenerator;
import org.flasck.flas.compiler.jvmgen.FunctionState;
import org.flasck.flas.compiler.jvmgen.JVMGenerator;
import org.flasck.flas.hsi.ArgSlot;
import org.flasck.flas.parsedForm.ContractDecl;
import org.flasck.flas.parsedForm.FieldsDefn.FieldsType;
import org.flasck.flas.parsedForm.FunctionCaseDefn;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.parsedForm.MakeSend;
import org.flasck.flas.parsedForm.Messages;
import org.flasck.flas.parsedForm.ObjectMethod;
import org.flasck.flas.parsedForm.SendMessage;
import org.flasck.flas.parsedForm.StandaloneMethod;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.StructField;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.parsedForm.TypedPattern;
import org.flasck.flas.parsedForm.UnresolvedOperator;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.parsedForm.VarPattern;
import org.flasck.flas.parser.ut.UnitDataDeclaration;
import org.flasck.flas.patterns.HSIArgsTree;
import org.flasck.flas.repository.LoadBuiltins;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.repository.StackVisitor;
import org.flasck.flas.repository.Traverser;
import org.flasck.jvm.J;
import org.hamcrest.Matchers;
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
import org.zinutils.bytecode.Var.AVar;
import org.zinutils.bytecode.mock.VarMatcher;

public class ExpressionGeneration {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	private InputPosition pos = new InputPosition("-", 1, 0, null);
	private final PackageName pkg = new PackageName("test.repo");
	private final MethodDefiner meth = context.mock(MethodDefiner.class);
	private final NestedVisitor nv = context.mock(NestedVisitor.class);
	private final StackVisitor sv = new StackVisitor();
	@SuppressWarnings("unchecked")
	private final List<IExpr> block = context.mock(List.class, "block");
	
	@Before
	public void setup() {
		sv.push(nv);
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
		IExpr exprValue = context.mock(IExpr.class, "expr");
		context.checking(new Expectations() {{
			oneOf(meth).aNull(); will(returnValue(dv));
			oneOf(meth).intConst(42); will(returnValue(iv));
			oneOf(meth).box(iv); will(returnValue(biv));
			oneOf(meth).castTo(dv, "java.lang.Double"); will(returnValue(cdv));
			oneOf(meth).makeNew("org.flasck.jvm.builtin.FLNumber", biv, cdv); will(returnValue(exprValue));
			oneOf(nv).result(exprValue);
		}});
		ExprGenerator eg = new ExprGenerator(new FunctionState(meth, null, null), sv, block);
		Traverser gen = new Traverser(eg).withHSI();
		gen.visitExpr(expr, 0);
	}

	@Test
	public void aSimpleString() {
		StringLiteral expr = new StringLiteral(pos, "hello");
		IExpr sval = context.mock(IExpr.class, "sval");
		context.checking(new Expectations() {{
			oneOf(meth).stringConst("hello"); will(returnValue(sval));
			oneOf(nv).result(sval);
		}});
		Traverser gen = new Traverser(new ExprGenerator(new FunctionState(meth, null, null), sv, block)).withHSI();
		gen.visitExpr(expr, 0);
	}

	@Test
	public void aVar() {
		UnresolvedVar expr = new UnresolvedVar(pos, "x");
		FunctionName nameX = FunctionName.function(pos, pkg, "x");
		expr.bind(new FunctionDefinition(nameX, 0));
		IExpr ev = context.mock(IExpr.class, "ev");
		context.checking(new Expectations() {{
			oneOf(meth).classConst("test.repo.PACKAGEFUNCTIONS$x"); will(returnValue(ev));
			oneOf(nv).result(ev);
		}});
		Traverser gen = new Traverser(new ExprGenerator(new FunctionState(meth, null, null), sv, block)).withHSI();
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
		IExpr assign = context.mock(IExpr.class, "assign");
		context.checking(new Expectations() {{
			oneOf(meth).arrayItem(J.OBJECT, ax, 1); will(returnValue(args));
			oneOf(meth).nextLocal(); will(returnValue(18));
			oneOf(meth).callStatic(J.FLEVAL, J.OBJECT, "head", cx, args); will(returnValue(head0));
			oneOf(meth).assign(with(VarMatcher.local(18)), with(head0)); will(returnValue(assign));
			oneOf(block).add(assign);
			oneOf(nv).result(with(VarMatcher.local(18)));
		}});
		FunctionState state = new FunctionState(meth, cx, null);
		state.bindVar(block, "p", new ArgSlot(1, null), null);
		ExprGenerator eg = new ExprGenerator(state, sv, block);
		sv.push(eg);
		Traverser gen = new Traverser(eg).withHSI();
		gen.visitExpr(expr, 2);
	}

	@Test
	public void aTypedFunctionArgument() {
		UnresolvedVar expr = new UnresolvedVar(pos, "p");
		FunctionName nameX = FunctionName.function(pos, pkg, "p");
		TypeReference string = new TypeReference(pos, "String");
		string.bind(LoadBuiltins.string);
		expr.bind(new TypedPattern(pos, string, new VarName(pos, nameX, "p")));
		Var ax = null;
		IExpr cx = context.mock(IExpr.class, "cx");
		IExpr args = context.mock(IExpr.class, "args");
		IExpr head0 = context.mock(IExpr.class, "head0");
		IExpr assign = context.mock(IExpr.class, "assign");
		context.checking(new Expectations() {{
			oneOf(meth).arrayItem(J.OBJECT, ax, 0); will(returnValue(args));
			oneOf(meth).nextLocal(); will(returnValue(18));
			oneOf(meth).callStatic(J.FLEVAL, J.OBJECT, "head", cx, args); will(returnValue(head0));
			oneOf(meth).assign(with(VarMatcher.local(18)), with(head0)); will(returnValue(assign));
			oneOf(block).add(assign);
			oneOf(nv).result(with(VarMatcher.local(18)));
		}});
		Traverser gen = new Traverser(new ExprGenerator(new FunctionState(meth, cx, null), sv, block)).withHSI();
		gen.visitExpr(expr, 2);
	}

	@Test
	public void aVarBoundToAUDDBoundToAString() {
		UnresolvedVar expr = new UnresolvedVar(pos, "p");
		FunctionName nameX = FunctionName.function(pos, pkg, "p");
		UnitDataDeclaration udd = new UnitDataDeclaration(pos, false, LoadBuiltins.stringTR, nameX, new StringLiteral(pos, "hello"));
		expr.bind(udd);
		IExpr cx = context.mock(IExpr.class, "cx");
		context.checking(new Expectations() {{
			oneOf(meth).nextLocal(); will(returnValue(76));
		}});
		Var mock = new AVar(meth, J.OBJECT, "mock");
		context.checking(new Expectations() {{
			oneOf(nv).result(mock);
		}});
		FunctionState fs = new FunctionState(meth, cx, null);
		fs.addMock(udd, mock);
		Traverser gen = new Traverser(new ExprGenerator(fs, sv, block)).withHSI();
		gen.visitExpr(expr, 0);
	}
	
	@Test
	public void aVarBoundToAUDDBoundToAContract() {
		UnresolvedVar expr = new UnresolvedVar(pos, "p");
		FunctionName nameX = FunctionName.function(pos, pkg, "p");
		TypeReference ctr = new TypeReference(pos, "MyContract");
		ContractDecl cd = new ContractDecl(pos, pos, new SolidName(pkg, "MyContract"));
		ctr.bind(cd);
		UnitDataDeclaration udd = new UnitDataDeclaration(pos, false, ctr, nameX, null);
		expr.bind(udd);
		IExpr cx = context.mock(IExpr.class, "cx");
		context.checking(new Expectations() {{
			oneOf(meth).nextLocal(); will(returnValue(76));
		}});
		Var mock = new AVar(meth, J.OBJECT, "mock");
		context.checking(new Expectations() {{
			oneOf(nv).result(mock);
		}});
		FunctionState fs = new FunctionState(meth, cx, null);
		fs.addMock(udd, mock);
		Traverser gen = new Traverser(new ExprGenerator(fs, sv, block)).withHSI();
		gen.visitExpr(expr, 0);
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
			oneOf(block).add(assign);
			oneOf(nv).result(var);
		}});
		new ExprGenerator(new FunctionState(meth, null, null), sv, block);
		Traverser gen = new Traverser(sv).withHSI();
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
			oneOf(meth).callStatic("org.flasck.jvm.fl.FLClosure", "org.flasck.jvm.fl.FLCurry", "curry", xAsObj, expArgs, args); will(returnValue(aev));
			oneOf(meth).avar("org.flasck.jvm.fl.FLClosure", "v1"); will(returnValue(var));
			oneOf(meth).assign(with(any(Var.class)), with(aev)); will(returnValue(assign));
			oneOf(block).add(assign);
			oneOf(nv).result(var);
		}});
		new ExprGenerator(new FunctionState(meth, null, null), sv, block);
		Traverser gen = new Traverser(sv).withHSI();
		gen.visitExpr(expr, 0);
	}

	@Test
	public void aStructConstructorWithNoArgsExpectingNoArgsBecomesAConstant() {
		UnresolvedVar expr = new UnresolvedVar(pos, "Ctor");
		expr.bind(new StructDefn(pos, FieldsType.STRUCT, "test.repo", "Ctor", true));
		IExpr fcx = context.mock(IExpr.class, "fcx");
		List<IExpr> argsList = new ArrayList<>();
		IExpr arr = context.mock(IExpr.class, "arr");
		IExpr call = context.mock(IExpr.class, "call");
		context.checking(new Expectations() {{
			oneOf(meth).arrayOf("java.lang.Object", argsList); will(returnValue(arr));
			oneOf(meth).callStatic("test.repo.Ctor", "java.lang.Object", "eval", fcx, arr); will(returnValue(call));
			oneOf(nv).result(call);
		}});
		Traverser gen = new Traverser(new ExprGenerator(new FunctionState(meth, fcx, null), sv, block)).withHSI();
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
		HSIArgsTree hsi = new HSIArgsTree(0);
		hsi.consider(fi);
		fn.bindHsi(hsi);
		List<IExpr> argsList = new ArrayList<>();
		IExpr arr = context.mock(IExpr.class, "arr");
		IExpr x = context.mock(IExpr.class, "Ctor");
		IExpr rv = context.mock(IExpr.class, "rv");
		context.checking(new Expectations() {{
			allowing(bcc).generateAssociatedSourceFile();
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
			oneOf(meth).returnObject(x); will(returnValue(rv));
			oneOf(rv).flush();
		}});
		StackVisitor sv = new StackVisitor();
		new JVMGenerator(bce, sv);
		Traverser gen = new Traverser(sv).withHSI();
		gen.visitFunction(fn);
	}

	@Test
	public void aStandaloneMethod() {
		ByteCodeStorage bce = context.mock(ByteCodeStorage.class);
		ByteCodeSink bcc = context.mock(ByteCodeSink.class);
		FunctionName fnName = FunctionName.standaloneMethod(pos, pkg, "f");
		UnresolvedVar expr = new UnresolvedVar(pos, "Ctor");
		expr.bind(new StructDefn(pos, FieldsType.STRUCT, "test.repo", "Ctor", true));
		ObjectMethod om = new ObjectMethod(pos, fnName, new ArrayList<>());
		om.sendMessage(new SendMessage(pos, expr));
		StandaloneMethod sm = new StandaloneMethod(om);
		FunctionIntro fi = new FunctionIntro(fnName, new ArrayList<>());
		FunctionCaseDefn fcd = new FunctionCaseDefn(null, expr);
		fi.functionCase(fcd);
		om.conversion(Arrays.asList(fi));
		HSIArgsTree hsi = new HSIArgsTree(0);
		hsi.consider(fi);
		om.bindHsi(hsi);
		List<IExpr> argsList = new ArrayList<>();
		IExpr arr = context.mock(IExpr.class, "arr");
		IExpr x = context.mock(IExpr.class, "Ctor");
		IExpr rv = context.mock(IExpr.class, "rv");
		context.checking(new Expectations() {{
			allowing(bcc).generateAssociatedSourceFile();
			oneOf(meth).nextLocal(); will(returnValue(22));
			oneOf(meth).nextLocal(); will(returnValue(23));
		}});
		Var cxt = new Var.AVar(meth, "org.ziniki.ziwsh.json.FLEvalContext", "cxt");
		Var args = new Var.AVar(meth, "JVMRunner", "runner");
		context.checking(new Expectations() {{
			oneOf(bce).newClass("test.repo.PACKAGEFUNCTIONS$f"); will(returnValue(bcc));
			oneOf(bcc).createMethod(true, "java.lang.Object", "eval"); will(returnValue(meth));
			oneOf(meth).argument("org.ziniki.ziwsh.json.FLEvalContext", "cxt"); will(returnValue(cxt));
			oneOf(meth).argument("[java.lang.Object", "args"); will(returnValue(args));
			oneOf(meth).arrayOf("java.lang.Object", argsList); will(returnValue(arr));
			oneOf(meth).callStatic("test.repo.Ctor", "java.lang.Object", "eval", cxt, arr); will(returnValue(x));
			oneOf(meth).returnObject(x); will(returnValue(rv));
			oneOf(rv).flush();
		}});
		StackVisitor sv = new StackVisitor();
		new JVMGenerator(bce, sv);
		Traverser gen = new Traverser(sv).withHSI();
		gen.visitStandaloneMethod(sm);
	}

	@Test
	public void anOp() {
		IExpr ret = context.mock(IExpr.class);
		context.checking(new Expectations() {{
			oneOf(meth).classConst("org.flasck.jvm.fl.FLEval$Plus"); will(returnValue(ret));
			oneOf(nv).result(ret);
		}});
		UnresolvedOperator expr = new UnresolvedOperator(pos, "+");
		expr.bind(new FunctionDefinition(FunctionName.function(pos, null, "+"), 2));
		Traverser gen = new Traverser(new ExprGenerator(new FunctionState(meth, null, null), sv, block)).withHSI();
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
		HSIArgsTree hsi = new HSIArgsTree(0);
		hsi.consider(fi);
		fn.bindHsi(hsi);
		List<IExpr> argsList = new ArrayList<>();
		IExpr arr = context.mock(IExpr.class, "arr");
		IExpr x = context.mock(IExpr.class, "Ctor");
		IExpr rx = context.mock(IExpr.class, "returnIt");
		context.checking(new Expectations() {{
			allowing(bcc).generateAssociatedSourceFile();
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
			oneOf(meth).returnObject(x); will(returnValue(rx));
			oneOf(rx).flush();
		}});
		StackVisitor sv = new StackVisitor();
		new JVMGenerator(bce, sv);
		Traverser gen = new Traverser(sv).withHSI();
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
		IExpr num = context.mock(IExpr.class, "num");
		IExpr strv = context.mock(IExpr.class, "strv");
		IExpr aev = context.mock(IExpr.class, "aev");
		IExpr args = context.mock(IExpr.class, "args");
		List<IExpr> argsList = new ArrayList<>();
		argsList.add(num);
		argsList.add(strv);
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
			oneOf(meth).makeNew("org.flasck.jvm.builtin.FLNumber", biv, cdv); will(returnValue(num));
			oneOf(meth).stringConst("hello"); will(returnValue(strv));
			oneOf(meth).arrayOf("java.lang.Object", argsList); will(returnValue(args));
			oneOf(meth).as(f, "java.lang.Object"); will(returnValue(fAsObj));
			oneOf(meth).callStatic("org.flasck.jvm.fl.FLClosure", "org.flasck.jvm.fl.FLClosure", "simple", fAsObj, args); will(returnValue(aev));
			oneOf(meth).avar("org.flasck.jvm.fl.FLClosure", "v1"); will(returnValue(var));
			oneOf(meth).assign(with(any(Var.class)), with(aev)); will(returnValue(assign));
			oneOf(block).add(assign);
			oneOf(nv).result(var);
		}});
		new ExprGenerator(new FunctionState(meth, null, null), sv, block);
		Traverser gen = new Traverser(sv).withHSI();
		gen.visitApplyExpr(ae);
	}

	@Test
	public void aStandaloneMethodApplication() {
		UnresolvedVar fn = new UnresolvedVar(pos, "f");
		FunctionName fnName = FunctionName.function(pos, pkg, "f");
		ObjectMethod om = new ObjectMethod(pos, fnName, new ArrayList<>());
		fn.bind(new StandaloneMethod(om));
		ApplyExpr ae = new ApplyExpr(pos, fn, new NumericLiteral(pos, "42", 2), new StringLiteral(pos, "hello"));
		IExpr f = context.mock(IExpr.class, "f");
		IExpr fAsObj = context.mock(IExpr.class, "fAsObj");
		IExpr dv = context.mock(IExpr.class, "dv");
		IExpr iv = context.mock(IExpr.class, "iv");
		IExpr biv = context.mock(IExpr.class, "biv");
		IExpr cdv = context.mock(IExpr.class, "cdv");
		IExpr num = context.mock(IExpr.class, "num");
		IExpr strv = context.mock(IExpr.class, "strv");
		IExpr aev = context.mock(IExpr.class, "aev");
		IExpr args = context.mock(IExpr.class, "args");
		List<IExpr> argsList = new ArrayList<>();
		argsList.add(num);
		argsList.add(strv);
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
			oneOf(meth).makeNew("org.flasck.jvm.builtin.FLNumber", biv, cdv); will(returnValue(num));
			oneOf(meth).stringConst("hello"); will(returnValue(strv));
			oneOf(meth).arrayOf("java.lang.Object", argsList); will(returnValue(args));
			oneOf(meth).as(f, "java.lang.Object"); will(returnValue(fAsObj));
			oneOf(meth).callStatic("org.flasck.jvm.fl.FLClosure", "org.flasck.jvm.fl.FLClosure", "simple", fAsObj, args); will(returnValue(aev));
			oneOf(meth).avar("org.flasck.jvm.fl.FLClosure", "v1"); will(returnValue(var));
			oneOf(meth).assign(with(any(Var.class)), with(aev)); will(returnValue(assign));
			oneOf(block).add(assign);
			oneOf(nv).result(var);
		}});
		new ExprGenerator(new FunctionState(meth, null, null), sv, block);
		Traverser gen = new Traverser(sv).withHSI();
		gen.visitApplyExpr(ae);
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
			oneOf(block).add(assignx);

			oneOf(meth).arrayOf("java.lang.Object", argsList); will(returnValue(args));
			oneOf(meth).as(f, "java.lang.Object"); will(returnValue(fAsObj));
			oneOf(meth).callStatic("org.flasck.jvm.fl.FLClosure", "org.flasck.jvm.fl.FLClosure", "simple", fAsObj, args); will(returnValue(aev));
			oneOf(meth).avar("org.flasck.jvm.fl.FLClosure", "v2"); will(returnValue(v2));
			oneOf(meth).assign(with(any(Var.class)), with(aev)); will(returnValue(assignae));
			oneOf(block).add(assignae);
			
			oneOf(nv).result(v2);
		}});
		new ExprGenerator(new FunctionState(meth, null, null), sv, block);
		Traverser gen = new Traverser(sv).withHSI();
		gen.visitApplyExpr(ae);
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
			
			oneOf(nv).result(nil);
		}});
		new ExprGenerator(new FunctionState(meth, fcx, null), sv, block);
		Traverser gen = new Traverser(sv).withHSI();
		gen.visitApplyExpr(ae);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void aFunctionApplicationWithExplicitCurrying() {
		UnresolvedVar fn = new UnresolvedVar(pos, "f");
		FunctionName fnName = FunctionName.function(pos, pkg, "f");
		fn.bind(new FunctionDefinition(fnName, 2));
		UnresolvedVar uv = new UnresolvedVar(pos, "_");
		uv.bind(LoadBuiltins.ca);
		ApplyExpr ae = new ApplyExpr(pos, fn, uv, new StringLiteral(pos, "hello"));
		context.checking(new Expectations() {{
			oneOf(meth).nextLocal(); will(returnValue(22));
		}});
		Var v1 = new Var.AVar(meth, "org.flasck.jvm.fl.FLClosure", "v1");
		context.assertIsSatisfied();
		IExpr f = context.mock(IExpr.class, "f");
		IExpr i1 = context.mock(IExpr.class, "i1");
		IExpr bi1 = context.mock(IExpr.class, "bi1");
		IExpr i2 = context.mock(IExpr.class, "i2");
		IExpr str = context.mock(IExpr.class, "str");
		IExpr bogus = context.mock(IExpr.class, "bogus"); // will not be used anywhere
		IExpr args = context.mock(IExpr.class, "args");
		IExpr xc = context.mock(IExpr.class, "xc");
		IExpr assign = context.mock(IExpr.class, "assign");
		context.checking(new Expectations() {{
			oneOf(meth).classConst("test.repo.PACKAGEFUNCTIONS$f"); will(returnValue(f));
			oneOf(meth).stringConst("hello"); will(returnValue(str));
			oneOf(meth).arrayOf(with(J.OBJECT), with(any(List.class))); will(returnValue(bogus));
			oneOf(meth).as(f, J.OBJECT); will(returnValue(f));
			oneOf(meth).intConst(2); will(returnValue(i2));
			oneOf(meth).intConst(1); will(returnValue(i1));
			oneOf(meth).box(i1); will(returnValue(bi1));
			oneOf(meth).arrayOf(with(J.OBJECT), (List)with(Matchers.contains(bi1, str))); will(returnValue(args));
			oneOf(meth).callStatic(J.FLCLOSURE, J.FLCURRY, "xcurry", f, i2, args); will(returnValue(xc));
			oneOf(meth).avar("org.flasck.jvm.fl.FLClosure", "v1"); will(returnValue(v1));
			oneOf(meth).assign(v1, xc); will(returnValue(assign));
			oneOf(block).add(assign);
			oneOf(nv).result(v1);
		}});
		new ExprGenerator(new FunctionState(meth, null, null), sv, block);
		Traverser gen = new Traverser(sv).withHSI();
		gen.visitApplyExpr(ae);
	}

	@Test
	public void aSpuriousApplyExprIsIgnored() {
		NumericLiteral nl = new NumericLiteral(pos, 42);
		ApplyExpr expr = new ApplyExpr(pos, nl);
		IExpr dv = context.mock(IExpr.class, "dv");
		IExpr iv = context.mock(IExpr.class, "iv");
		IExpr biv = context.mock(IExpr.class, "biv");
		IExpr cdv = context.mock(IExpr.class, "cdv");
		IExpr ret = context.mock(IExpr.class, "ret");
		context.checking(new Expectations() {{
			oneOf(meth).aNull(); will(returnValue(dv));
			oneOf(meth).intConst(42); will(returnValue(iv));
			oneOf(meth).box(iv); will(returnValue(biv));
			oneOf(meth).castTo(dv, "java.lang.Double"); will(returnValue(cdv));
			oneOf(meth).makeNew("org.flasck.jvm.builtin.FLNumber", biv, cdv); will(returnValue(ret));
			oneOf(nv).result(ret);
		}});
		Traverser gen = new Traverser(new ExprGenerator(new FunctionState(meth, null, null), sv, block)).withHSI();
		gen.visitExpr(expr, 0);
	}

	@Test
	public void anEmptyListIsNil() {
		IExpr fcx = context.mock(IExpr.class, "fcx");
		UnresolvedOperator op = new UnresolvedOperator(pos, "[]");
		op.bind(LoadBuiltins.nil);
		ApplyExpr expr = new ApplyExpr(pos, op);
		IExpr args = context.mock(IExpr.class, "args");
		IExpr clos = context.mock(IExpr.class, "clos");
		context.checking(new Expectations() {{
			oneOf(meth).arrayOf(J.OBJECT, new ArrayList<>()); will(returnValue(args));
			oneOf(meth).callStatic(J.NIL, J.OBJECT, "eval", fcx, args); will(returnValue(clos));
			oneOf(nv).result(clos);
		}});
		Traverser gen = new Traverser(new ExprGenerator(new FunctionState(meth, fcx, null), sv, block)).withHSI();
		gen.visitExpr(expr, 0);
	}
	
	@Test
	public void aSingletonListUsesMakeArray() {
		IExpr fcx = context.mock(IExpr.class, "fcx");
		UnresolvedOperator op = new UnresolvedOperator(pos, "[]");
		op.bind(LoadBuiltins.nil);
		NumericLiteral nl = new NumericLiteral(pos, "42", 2);
		ApplyExpr expr = new ApplyExpr(pos, op, nl);
		context.checking(new Expectations() {{
			oneOf(meth).nextLocal(); will(returnValue(2));
		}});

		IExpr i1 = context.mock(IExpr.class, "i1");
		IExpr args = context.mock(IExpr.class, "args");
		IExpr clos = context.mock(IExpr.class, "clos");
		IExpr assign = context.mock(IExpr.class, "assign");
		Var v1 = new AVar(meth, J.FLCLOSURE, "v1");
		context.checking(new Expectations() {{
			oneOf(meth).aNull(); will(returnValue(i1));
			oneOf(meth).intConst(42); will(returnValue(i1));
			oneOf(meth).box(i1); will(returnValue(i1));
			oneOf(meth).castTo(i1, "java.lang.Double"); will(returnValue(i1));
			oneOf(meth).makeNew("org.flasck.jvm.builtin.FLNumber", i1, i1); will(returnValue(i1));
			oneOf(meth).arrayOf(J.OBJECT, Arrays.asList(i1)); will(returnValue(args));
			oneOf(meth).callStatic(J.FLEVAL, J.OBJECT, "makeArray", fcx, args); will(returnValue(clos));
			oneOf(meth).avar(J.FLCLOSURE, "v1"); will(returnValue(v1));
			oneOf(meth).assign(v1, clos); will(returnValue(assign));
			oneOf(block).add(assign);
			oneOf(nv).result(v1);
		}});
		new ExprGenerator(new FunctionState(meth, fcx, null), sv, block);
		Traverser gen = new Traverser(sv).withHSI();
		gen.visitApplyExpr(expr);
	}

	@Test
	public void messagesPushesNestedExprGeneratorAndThenCollectsAList() {
		IExpr fcx = context.mock(IExpr.class, "fcx");
		NumericLiteral msg1 = new NumericLiteral(pos, "42", 2);
		NumericLiteral msg2 = new NumericLiteral(pos, "84", 2);
		Messages me = new Messages(pos, Arrays.asList(msg1, msg2));
		context.checking(new Expectations() {{
			oneOf(meth).nextLocal(); will(returnValue(2));
		}});

		IExpr i1 = context.mock(IExpr.class, "i1");
		IExpr i2 = context.mock(IExpr.class, "i2");
		IExpr args = context.mock(IExpr.class, "args");
		IExpr clos = context.mock(IExpr.class, "clos");
		IExpr assign = context.mock(IExpr.class, "assign");
		Var v1 = new AVar(meth, J.FLCLOSURE, "v1");
		context.checking(new Expectations() {{
			oneOf(meth).aNull(); will(returnValue(i1));
			oneOf(meth).intConst(42); will(returnValue(i1));
			oneOf(meth).box(i1); will(returnValue(i1));
			oneOf(meth).castTo(i1, "java.lang.Double"); will(returnValue(i1));
			oneOf(meth).makeNew("org.flasck.jvm.builtin.FLNumber", i1, i1); will(returnValue(i1));
			oneOf(meth).aNull(); will(returnValue(i2));
			oneOf(meth).intConst(84); will(returnValue(i2));
			oneOf(meth).box(i2); will(returnValue(i2));
			oneOf(meth).castTo(i2, "java.lang.Double"); will(returnValue(i2));
			oneOf(meth).makeNew("org.flasck.jvm.builtin.FLNumber", i2, i2); will(returnValue(i2));
			oneOf(meth).arrayOf(J.OBJECT, Arrays.asList(i1, i2)); will(returnValue(args));
			oneOf(meth).callStatic(J.FLEVAL, J.OBJECT, "makeArray", fcx, args); will(returnValue(clos));
			oneOf(meth).avar(J.FLCLOSURE, "v1"); will(returnValue(v1));
			oneOf(meth).assign(v1, clos); will(returnValue(assign));
			oneOf(block).add(assign);
			oneOf(nv).result(v1);
		}});
		new ExprGenerator(new FunctionState(meth, fcx, null), sv, block);
		Traverser gen = new Traverser(sv).withHSI();
		gen.visitMessages(me);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void aDotOperator() {
		UnresolvedVar from = new UnresolvedVar(pos, "from");
		TypeReference ctr = new TypeReference(pos, "Ctr");
		ContractDecl cd = new ContractDecl(pos, pos, new SolidName(pkg, "Ctr"));
		ctr.bind(cd);
		TypedPattern tp = new TypedPattern(pos, ctr, new VarName(pos, cd.name(), "from"));
		from.bind(tp);
		MakeSend ms = new MakeSend(pos, FunctionName.contractMethod(pos, cd.name(), "f"), from, 0);
		StackVisitor stackv = new StackVisitor();
		stackv.push(nv);
		context.checking(new Expectations() {{
			oneOf(meth).nextLocal(); will(returnValue(22));
		}});
		IExpr fcx = context.mock(IExpr.class, "fcx");
		IExpr ai = context.mock(IExpr.class, "ai");
		IExpr ass = context.mock(IExpr.class, "ass");
		Var fargs = new AVar(meth, J.OBJECT, "v1");
		IExpr sendClz = context.mock(IExpr.class, "clz");
		IExpr sendMeth = context.mock(IExpr.class, "meth");
		IExpr i0 = context.mock(IExpr.class, "0");
		context.checking(new Expectations() {{
			oneOf(meth).arrayItem(J.OBJECT, fargs, 0); will(returnValue(ai));
			oneOf(meth).nextLocal(); will(returnValue(23));
			oneOf(meth).callStatic(J.FLEVAL, J.OBJECT, "head", fcx, ai); will(returnValue(ai));
			oneOf(meth).assign(with(VarMatcher.local(23)), with(ai)); will(returnValue(ass));
			oneOf(block).add(ass);
			oneOf(meth).classConst("test.repo.Ctr$Up"); will(returnValue(sendClz));
			oneOf(meth).stringConst("f"); will(returnValue(sendMeth));
			oneOf(meth).intConst(0); will(returnValue(i0));
			oneOf(meth).callInterface(with(J.OBJECT), with(fcx), with("mksend"), with(Matchers.array(Matchers.is(sendClz), Matchers.is(sendMeth), VarMatcher.local(23), Matchers.is(i0))));
		}});
		new ApplyExprGenerator(new FunctionState(meth, fcx, fargs), sv, block);
		Traverser gen = new Traverser(sv).withHSI();
		gen.visitExpr(ms, 0);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void applyingADotOperator() {
		UnresolvedVar from = new UnresolvedVar(pos, "from");
		TypeReference ctr = new TypeReference(pos, "Ctr");
		ContractDecl cd = new ContractDecl(pos, pos, new SolidName(pkg, "Ctr"));
		ctr.bind(cd);
		TypedPattern tp = new TypedPattern(pos, ctr, new VarName(pos, cd.name(), "from"));
		from.bind(tp);
		MakeSend ms = new MakeSend(pos, FunctionName.contractMethod(pos, cd.name(), "f"), from, 2);
		ApplyExpr ae = new ApplyExpr(pos, ms, new NumericLiteral(pos, "42", 2), new StringLiteral(pos, "hello"));
		StackVisitor stackv = new StackVisitor();
		stackv.push(nv);
		context.checking(new Expectations() {{
			oneOf(meth).nextLocal(); will(returnValue(22));
			oneOf(meth).nextLocal(); will(returnValue(27));
		}});
		IExpr fcx = context.mock(IExpr.class, "fcx");
		IExpr ai = context.mock(IExpr.class, "ai");
		IExpr ass = context.mock(IExpr.class, "ass");
		Var fargs = new AVar(meth, J.OBJECT, "fargs");
		Var v1 = new AVar(meth, J.OBJECT, "v1");
		IExpr sendClz = context.mock(IExpr.class, "clz");
		IExpr sendMeth = context.mock(IExpr.class, "meth");
		IExpr i2 = context.mock(IExpr.class, "2");
		IExpr msi = context.mock(IExpr.class, "msi");
		IExpr n1 = context.mock(IExpr.class, "n1");
		IExpr shello = context.mock(IExpr.class, "hello");
		IExpr args = context.mock(IExpr.class, "args");
		IExpr clos = context.mock(IExpr.class, "clos");
		IExpr ass2 = context.mock(IExpr.class, "ass2");
		context.checking(new Expectations() {{
			oneOf(meth).arrayItem(J.OBJECT, fargs, 0); will(returnValue(ai));
			oneOf(meth).nextLocal(); will(returnValue(23));
			oneOf(meth).callStatic(J.FLEVAL, J.OBJECT, "head", fcx, ai); will(returnValue(ai));
			oneOf(meth).assign(with(VarMatcher.local(23)), with(ai)); will(returnValue(ass));
			oneOf(block).add(ass);
			oneOf(meth).classConst("test.repo.Ctr$Up"); will(returnValue(sendClz));
			oneOf(meth).stringConst("f"); will(returnValue(sendMeth));
			oneOf(meth).intConst(2); will(returnValue(i2));
			oneOf(meth).callInterface(with(J.OBJECT), with(fcx), with("mksend"), with(Matchers.array(Matchers.is(sendClz), Matchers.is(sendMeth), VarMatcher.local(23), Matchers.is(i2)))); will(returnValue(msi));
			oneOf(meth).aNull(); will(returnValue(n1));
			oneOf(meth).intConst(42); will(returnValue(n1));
			oneOf(meth).box(n1); will(returnValue(n1));
			oneOf(meth).castTo(n1, "java.lang.Double"); will(returnValue(n1));
			oneOf(meth).makeNew("org.flasck.jvm.builtin.FLNumber", n1, n1); will(returnValue(n1));
			oneOf(meth).stringConst("hello"); will(returnValue(shello));
			oneOf(meth).arrayOf(J.OBJECT, Arrays.asList(n1, shello)); will(returnValue(args));
			oneOf(meth).as(msi, J.OBJECT); will(returnValue(msi));
			oneOf(meth).callStatic(J.FLCLOSURE, J.FLCLOSURE, "simple", msi, args); will(returnValue(clos));
			oneOf(meth).avar(J.FLCLOSURE, "v1"); will(returnValue(v1));
			oneOf(meth).assign(v1, clos); will(returnValue(ass2));
			oneOf(block).add(ass2);
			oneOf(nv).result(v1);
		}});
		new ExprGenerator(new FunctionState(meth, fcx, fargs), sv, block);
		Traverser gen = new Traverser(sv).withHSI();
		gen.visitApplyExpr(ae);
	}
}
