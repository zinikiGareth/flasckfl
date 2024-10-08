package test.flas.generator.js;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.ApplyExpr;
import org.flasck.flas.commonBase.MemberExpr;
import org.flasck.flas.commonBase.NumericLiteral;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.commonBase.names.SolidName;
import org.flasck.flas.commonBase.names.VarName;
import org.flasck.flas.compiler.jsgen.ApplyExprGeneratorJS;
import org.flasck.flas.compiler.jsgen.ExprGeneratorJS;
import org.flasck.flas.compiler.jsgen.JSFunctionState;
import org.flasck.flas.compiler.jsgen.JSGenerator;
import org.flasck.flas.compiler.jsgen.JSGenerator.XCArg;
import org.flasck.flas.compiler.jsgen.creators.JSBlockCreator;
import org.flasck.flas.compiler.jsgen.creators.JSMethodCreator;
import org.flasck.flas.compiler.jsgen.form.JSExpr;
import org.flasck.flas.compiler.jsgen.form.JSLiteral;
import org.flasck.flas.compiler.jsgen.form.JSString;
import org.flasck.flas.compiler.jsgen.packaging.JSStorage;
import org.flasck.flas.parsedForm.AnonymousVar;
import org.flasck.flas.parsedForm.ContractDecl;
import org.flasck.flas.parsedForm.ContractDecl.ContractType;
import org.flasck.flas.parsedForm.FieldsDefn.FieldsType;
import org.flasck.flas.parsedForm.FunctionCaseDefn;
import org.flasck.flas.parsedForm.FunctionConstness;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.parsedForm.MakeAcor;
import org.flasck.flas.parsedForm.MakeSend;
import org.flasck.flas.parsedForm.Messages;
import org.flasck.flas.parsedForm.ObjectDefn;
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
import org.hamcrest.Matchers;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Rule;
import org.junit.Test;

public class ExpressionGenerationJS {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	private JSMethodCreator meth = context.mock(JSMethodCreator.class);
	private NestedVisitor nv = context.mock(NestedVisitor.class);
	private InputPosition pos = new InputPosition("-", 1, 0, null, null);
	private final PackageName pkg = new PackageName("test.repo");
	private JSFunctionState state = context.mock(JSFunctionState.class);
	private JSExpr res = context.mock(JSExpr.class, "result");
	private final FunctionIntro intro = null;

	@Test
	public void aSimpleInteger() {
		JSExpr r = context.mock(JSExpr.class, "r");
		context.checking(new Expectations() {{
			oneOf(meth).literal("42"); will(returnValue(r));
			oneOf(nv).result(r);
		}});
		NumericLiteral expr = new NumericLiteral(pos, "42", 2);
		StackVisitor stackv = new StackVisitor();
		stackv.push(nv);
		new ExprGeneratorJS(state, stackv, meth, false);
		Traverser gen = new Traverser(stackv).withHSI();
		gen.visitExpr(expr, 0);
	}

	@Test
	public void aSimpleString() {
		JSString r = new JSString("s");
		context.checking(new Expectations() {{
			oneOf(meth).string("hello"); will(returnValue(r));
			oneOf(nv).result(r);
		}});
		StringLiteral expr = new StringLiteral(pos, "hello");
		StackVisitor stackv = new StackVisitor();
		stackv.push(nv);
		new ExprGeneratorJS(state, stackv, meth, false);
		Traverser gen = new Traverser(stackv).withHSI();
		gen.visitExpr(expr, 0);
	}

	@Test
	public void aVarBoundToAFunction() {
		JSExpr r = context.mock(JSExpr.class, "r");
		UnresolvedVar expr = new UnresolvedVar(pos, "x");
		FunctionName nameX = FunctionName.function(pos, pkg, "x");
		context.checking(new Expectations() {{
			oneOf(meth).pushFunction("test__repo.x", nameX, 2); will(returnValue(r));
			oneOf(nv).result(r);
		}});
		expr.bind(new FunctionDefinition(nameX, 0, null));
		StackVisitor stackv = new StackVisitor();
		stackv.push(nv);
		new ExprGeneratorJS(state, stackv, meth, false);
		Traverser gen = new Traverser(stackv).withHSI();
		gen.visitExpr(expr, 2);
	}
	
	@Test
	public void aVarBoundToAFunctionArgument() {
		UnresolvedVar expr = new UnresolvedVar(pos, "p");
		FunctionName nameX = FunctionName.function(pos, pkg, "p");
		expr.bind(new VarPattern(pos, new VarName(pos, nameX, "p")));
		JSExpr r = context.mock(JSExpr.class, "r");
		context.checking(new Expectations() {{
			oneOf(meth).boundVar("p"); will(returnValue(r));
			oneOf(nv).result(r);
		}});
		StackVisitor stackv = new StackVisitor();
		stackv.push(nv);
		new ExprGeneratorJS(state, stackv, meth, false);
		Traverser gen = new Traverser(stackv).withHSI();
		gen.visitExpr(expr, 2);
	}
	
	@Test
	public void aVarBoundToATypedFunctionArgument() {
		UnresolvedVar expr = new UnresolvedVar(pos, "p");
		FunctionName nameX = FunctionName.function(pos, pkg, "p");
		expr.bind(new TypedPattern(pos, LoadBuiltins.stringTR, new VarName(pos, nameX, "p")));
		JSExpr r = context.mock(JSExpr.class, "r");
		context.checking(new Expectations() {{
			oneOf(meth).boundVar("p"); will(returnValue(r));
			oneOf(nv).result(r);
		}});
		StackVisitor stackv = new StackVisitor();
		stackv.push(nv);
		new ExprGeneratorJS(state, stackv, meth, false);
		Traverser gen = new Traverser(stackv).withHSI();
		gen.visitExpr(expr, 2);
	}
	
	@Test
	public void aVarBoundToAStructFieldInsideAnAccessor() {
		UnresolvedVar expr = new UnresolvedVar(pos, "p");
		StructDefn ctr = new StructDefn(pos, FieldsType.STRUCT, pkg.uniqueName(), "All", true);
		StructField sf = new StructField(pos, ctr, false, true, LoadBuiltins.stringTR, "x");
		sf.fullName(new VarName(pos, pkg, "x"));
		expr.bind(sf);
		JSExpr r = context.mock(JSExpr.class, "r");
		JSExpr c = context.mock(JSExpr.class, "container");
		context.checking(new Expectations() {{
			oneOf(state).container(ctr.name()); will(returnValue(c));
			oneOf(meth).loadField(c, "x"); will(returnValue(r));
			oneOf(nv).result(r);
		}});
		StackVisitor stackv = new StackVisitor();
		stackv.push(nv);
		new ExprGeneratorJS(state, stackv, meth, false);
		Traverser gen = new Traverser(stackv).withHSI();
		gen.visitExpr(expr, 0);
	}
	
	@Test
	public void aVarBoundToAUDDBoundToAString() {
		UnresolvedVar expr = new UnresolvedVar(pos, "p");
		FunctionName nameX = FunctionName.function(pos, pkg, "p");
		JSLiteral sl = new JSLiteral("hello");
		UnitDataDeclaration udd = new UnitDataDeclaration(pos, false, LoadBuiltins.stringTR, nameX, new StringLiteral(pos, "hello"));
		expr.bind(udd);
		context.checking(new Expectations() {{
			oneOf(state).resolveMock(with(any(JSMethodCreator.class)), with(udd)); will(returnValue(sl));
			oneOf(nv).result(sl);
		}});
		StackVisitor stackv = new StackVisitor();
		stackv.push(nv);
		new ExprGeneratorJS(state, stackv, meth, false);
		Traverser gen = new Traverser(stackv).withHSI();
		gen.visitExpr(expr, 0);
	}
	
	@Test
	public void aVarBoundToAUDDBoundToAContract() {
		UnresolvedVar expr = new UnresolvedVar(pos, "p");
		FunctionName nameX = FunctionName.function(pos, pkg, "p");
		TypeReference ctr = new TypeReference(pos, "MyContract");
		ContractDecl cd = new ContractDecl(pos, pos, ContractType.CONTRACT, new SolidName(pkg, "MyContract"));
		ctr.bind(cd);
		UnitDataDeclaration udd = new UnitDataDeclaration(pos, false, ctr, nameX, null);
		expr.bind(udd);
		JSExpr mc = context.mock(JSExpr.class, "mockContract");
		context.checking(new Expectations() {{
			oneOf(state).resolveMock(with(any(JSBlockCreator.class)), with(udd)); will(returnValue(mc));
			oneOf(nv).result(mc);
		}});
		StackVisitor stackv = new StackVisitor();
		stackv.push(nv);
		new ExprGeneratorJS(state, stackv, meth, false);
		Traverser gen = new Traverser(stackv).withHSI();
		gen.visitExpr(expr, 0);
	}
	
	@Test
	public void aVarWithNoArgsExpectingNoArgsBecomesAClosureByItself() {
		JSExpr x = context.mock(JSExpr.class, "f");
		JSExpr clos = context.mock(JSExpr.class, "clos");
		UnresolvedVar expr = new UnresolvedVar(pos, "x");
		FunctionName nameX = FunctionName.function(pos, pkg, "x");
		context.checking(new Expectations() {{
			oneOf(meth).pushFunction("test__repo.x", nameX, 0); will(returnValue(x));
			oneOf(meth).closure(false, x); will(returnValue(clos));
			oneOf(nv).result(clos);
		}});
		expr.bind(new FunctionDefinition(nameX, 0, null));
		StackVisitor stackv = new StackVisitor();
		stackv.push(nv);
		new ExprGeneratorJS(state, stackv, meth, false);
		Traverser gen = new Traverser(stackv).withHSI();
		gen.visitExpr(expr, 0);
	}

	@Test
	public void aVarWithNoArgsExpectingTwoArgsBecomesACurriedFunction() {
		JSExpr x = context.mock(JSExpr.class, "f");
		JSExpr clos = context.mock(JSExpr.class, "clos");
		UnresolvedVar expr = new UnresolvedVar(pos, "x");
		FunctionName nameX = FunctionName.function(pos, pkg, "x");
		context.checking(new Expectations() {{
			oneOf(meth).pushFunction("test__repo.x", nameX, 2); will(returnValue(x));
			oneOf(meth).curry(false, 2, x); will(returnValue(clos));
			oneOf(nv).result(clos);
		}});
		expr.bind(new FunctionDefinition(nameX, 2, null));
		StackVisitor stackv = new StackVisitor();
		stackv.push(nv);
		new ExprGeneratorJS(state, stackv, meth, false);
		Traverser gen = new Traverser(stackv).withHSI();
		gen.visitExpr(expr, 0);
	}

	@Test
	public void aStructConstructorWithNoArgsExpectingNoArgsBecomesAConstant() {
		JSExpr x = context.mock(JSExpr.class, "f");
		context.checking(new Expectations() {{
			oneOf(meth).structConst(new SolidName(LoadBuiltins.builtinPkg, "test__repo.Ctor")); will(returnValue(x));
			oneOf(nv).result(x);
		}});
		UnresolvedVar expr = new UnresolvedVar(pos, "Ctor");
		expr.bind(new StructDefn(pos, FieldsType.STRUCT, "test__repo", "Ctor", true));
		StackVisitor stackv = new StackVisitor();
		stackv.push(nv);
		new ExprGeneratorJS(state, stackv, meth, false);
		Traverser gen = new Traverser(stackv).withHSI();
		gen.visitExpr(expr, 0);
	}

	@Test
	public void aStructConstructorWithNoArgsExpectingTwoArgsBecomesACurry() {
		JSExpr cons = context.mock(JSExpr.class, "cons");
		JSExpr curry = context.mock(JSExpr.class, "curry");
		context.checking(new Expectations() {{
			oneOf(meth).pushConstructor(new SolidName(LoadBuiltins.builtinPkg, "Cons"), "Cons"); will(returnValue(cons));
			oneOf(meth).curry(false, 2, cons); will(returnValue(curry));
			oneOf(nv).result(curry);
		}});
		UnresolvedVar expr = new UnresolvedVar(pos, "Cons");
		expr.bind(LoadBuiltins.cons);
		StackVisitor stackv = new StackVisitor();
		stackv.push(nv);
		new ExprGeneratorJS(state, stackv, meth, false);
		Traverser gen = new Traverser(stackv).withHSI();
		gen.visitExpr(expr, 0);
	}

	@Test
	public void aStructConstructorWithOneArgExpectingTwoArgsIsPushedByExprGenerator() {
		JSExpr cons = context.mock(JSExpr.class, "cons");
		context.checking(new Expectations() {{
			oneOf(meth).pushConstructor(new SolidName(LoadBuiltins.builtinPkg, "Cons"), "Cons"); will(returnValue(cons));
			oneOf(nv).result(cons);
		}});
		UnresolvedVar expr = new UnresolvedVar(pos, "Cons");
		expr.bind(LoadBuiltins.cons);
		StackVisitor stackv = new StackVisitor();
		stackv.push(nv);
		new ExprGeneratorJS(state, stackv, meth, false);
		Traverser gen = new Traverser(stackv).withHSI();
		gen.visitExpr(expr, 1);
	}

	@Test
	public void aSimpleFunction() {
		JSStorage jss = context.mock(JSStorage.class);
		JSExpr nret = context.mock(JSExpr.class, "nret");
		FunctionName name = FunctionName.function(pos, pkg, "x");
		context.checking(new Expectations() {{
			oneOf(jss).ensurePackageExists(new PackageName("test.repo"), "test.repo");
			oneOf(jss).newFunction(name, new PackageName("test.repo"), new PackageName("test.repo"), false, "x"); will(returnValue(meth));
			oneOf(meth).argument("_cxt");
			oneOf(meth).argumentList();
			oneOf(meth).checkCached();
			oneOf(meth).cacheResult(with(any(JSExpr.class)));
			oneOf(meth).structConst(new SolidName(LoadBuiltins.builtinPkg, "test.repo.Ctor")); will(returnValue(nret));
			oneOf(meth).returnObject(nret);
		}});
		StackVisitor sv = new StackVisitor();
		new JSGenerator(null, jss, sv, null);
		FunctionDefinition fn = new FunctionDefinition(name, 0, null);
		fn.setConstness(new FunctionConstness((String)null));
		FunctionIntro fi = new FunctionIntro(name, new ArrayList<>());
		UnresolvedVar expr = new UnresolvedVar(pos, "Ctor");
		expr.bind(new StructDefn(pos, FieldsType.STRUCT, "test.repo", "Ctor", true));
		FunctionCaseDefn fcd = new FunctionCaseDefn(pos, intro, null, expr);
		fi.functionCase(fcd);
		fn.intro(fi);
		HSIArgsTree hsi = new HSIArgsTree(0);
		hsi.consider(fi);
		fn.bindHsi(hsi);
		new Traverser(sv).withHSI().visitFunction(fn);
	}

	@Test
	public void aStandaloneMethod() {
		JSStorage jss = context.mock(JSStorage.class);
		JSExpr nret = context.mock(JSExpr.class, "nret");
		FunctionName fnName = FunctionName.standaloneMethod(pos, pkg, "f");
		context.checking(new Expectations() {{
			oneOf(jss).ensurePackageExists(new PackageName("test.repo"), "test.repo");
			oneOf(jss).newFunction(fnName, new PackageName("test.repo"), new PackageName("test.repo"), false, "f"); will(returnValue(meth));
			oneOf(meth).argumentList();
			oneOf(meth).argument("_cxt");
			oneOf(meth).structConst(new SolidName(LoadBuiltins.builtinPkg, "test.repo.Ctor")); will(returnValue(nret));
			oneOf(meth).returnObject(nret);
		}});
		StackVisitor sv = new StackVisitor();
		new JSGenerator(null, jss, sv, null);
		UnresolvedVar expr = new UnresolvedVar(pos, "Ctor");
		expr.bind(new StructDefn(pos, FieldsType.STRUCT, "test.repo", "Ctor", true));
		ObjectMethod om = new ObjectMethod(pos, fnName, new ArrayList<>(), null, null);
		om.sendMessage(new SendMessage(pos, expr));
		StandaloneMethod sm = new StandaloneMethod(om);
		FunctionIntro fi = new FunctionIntro(fnName, new ArrayList<>());
		FunctionCaseDefn fcd = new FunctionCaseDefn(pos, intro, null, expr);
		fi.functionCase(fcd);
		om.conversion(Arrays.asList(fi));
		HSIArgsTree hsi = new HSIArgsTree(0);
		hsi.consider(fi);
		om.bindHsi(hsi);
		new Traverser(sv).withHSI().visitStandaloneMethod(sm);
	}

	@Test
	public void anOp() {
		JSExpr r = context.mock(JSExpr.class, "r");
		UnresolvedOperator expr = new UnresolvedOperator(pos, "+");
		FunctionName plus = FunctionName.function(pos, null, "+");
		context.checking(new Expectations() {{
			oneOf(meth).pushFunction("FLBuiltin.plus", plus, 2); will(returnValue(r));
			oneOf(nv).result(r);
		}});
		expr.bind(new FunctionDefinition(plus, 2, null));
		StackVisitor stackv = new StackVisitor();
		stackv.push(nv);
		new ExprGeneratorJS(state, stackv, meth, false);
		Traverser gen = new Traverser(stackv).withHSI();
		gen.visitExpr(expr, 2);
	}

	@Test
	public void aFunctionApplication() {
		UnresolvedVar fn = new UnresolvedVar(pos, "f");
		FunctionName fnName = FunctionName.function(pos, pkg, "f");
		fn.bind(new FunctionDefinition(fnName, 2, null));
		ApplyExpr ae = new ApplyExpr(pos, fn, new NumericLiteral(pos, "42", 2), new StringLiteral(pos, "hello"));
		JSExpr f = context.mock(JSExpr.class, "f");
		JSExpr iv = context.mock(JSExpr.class, "iv");
		JSString sv = new JSString("s");
		StackVisitor stackv = new StackVisitor();
		stackv.push(nv);
		context.checking(new Expectations() {{
			oneOf(meth).pushFunction("test__repo.f", fnName, 2); will(returnValue(f));
			oneOf(meth).literal("42"); will(returnValue(iv));
			oneOf(meth).string("hello"); will(returnValue(sv));
			oneOf(meth).closure(false, f, iv, sv); will(returnValue(res));
			oneOf(nv).result(res);
		}});
		new ExprGeneratorJS(state, stackv, meth, false);
		Traverser gen = new Traverser(stackv).withHSI();
		gen.visitApplyExpr(ae);
	}

	@Test
	public void aStandaloneMethodApplication() {
		UnresolvedVar fn = new UnresolvedVar(pos, "f");
		FunctionName fnName = FunctionName.standaloneMethod(pos, pkg, "f");
		ObjectMethod om = new ObjectMethod(pos, fnName, new ArrayList<>(), null, null);
		fn.bind(new StandaloneMethod(om));
		ApplyExpr ae = new ApplyExpr(pos, fn, new NumericLiteral(pos, "42", 2), new StringLiteral(pos, "hello"));
		JSExpr f = context.mock(JSExpr.class, "f");
		JSExpr iv = context.mock(JSExpr.class, "iv");
		JSString sv = new JSString("s");
		StackVisitor stackv = new StackVisitor();
		stackv.push(nv);
		context.checking(new Expectations() {{
			oneOf(meth).pushFunction("test__repo.f", fnName, 2); will(returnValue(f));
			oneOf(meth).literal("42"); will(returnValue(iv));
			oneOf(meth).string("hello"); will(returnValue(sv));
			oneOf(meth).closure(false, f, iv, sv); will(returnValue(res));
			oneOf(nv).result(res);
		}});
		new ExprGeneratorJS(state, stackv, meth, false);
		Traverser gen = new Traverser(stackv).withHSI();
		gen.visitApplyExpr(ae);
	}

	@Test
	public void aNestedFunctionApplication() {
		UnresolvedVar fn = new UnresolvedVar(pos, "f");
		FunctionName fnName = FunctionName.function(pos, pkg, "f");
		fn.bind(new FunctionDefinition(fnName, 1, null));
		UnresolvedVar var = new UnresolvedVar(pos, "x");
		FunctionName varName = FunctionName.function(pos, pkg, "x");
		var.bind(new FunctionDefinition(varName, 0, null));
		ApplyExpr ae = new ApplyExpr(pos, fn, var);
		JSExpr f = context.mock(JSExpr.class, "f");
		JSExpr x = context.mock(JSExpr.class, "x");
		JSExpr v1 = context.mock(JSExpr.class, "v1");
		StackVisitor stackv = new StackVisitor();
		stackv.push(nv);
		context.checking(new Expectations() {{
			oneOf(meth).pushFunction("test__repo.x", varName, 0); will(returnValue(x));
			oneOf(meth).closure(false, x); will(returnValue(v1));
			oneOf(meth).pushFunction("test__repo.f", fnName, 1); will(returnValue(f));
			oneOf(meth).closure(false, f, v1); will(returnValue(res));
			oneOf(nv).result(res);
		}});
		new ExprGeneratorJS(state, stackv, meth, false);
		Traverser gen = new Traverser(stackv).withHSI();
		gen.visitApplyExpr(ae);
	}

	@Test
	public void aConstructorApplicationWithArgs() {
		UnresolvedVar nilOp = new UnresolvedVar(pos, "Nil");
		UnresolvedVar fn = new UnresolvedVar(pos, "Cons");
		StructDefn nilT = new StructDefn(pos, pos, FieldsType.STRUCT, new SolidName(LoadBuiltins.builtinPkg, "Nil"), false, new ArrayList<>());
		StructDefn consT = new StructDefn(pos, pos, FieldsType.STRUCT, new SolidName(LoadBuiltins.builtinPkg, "Cons"), false, new ArrayList<>());
		consT.addField(new StructField(pos, consT, false, true, new TypeReference(pos, "A"), "head"));
		consT.addField(new StructField(pos, consT, false, true, new TypeReference(pos, "List", new TypeReference(pos, "A")), "tail"));
		fn.bind(consT);
		nilOp.bind(nilT);
		ApplyExpr ae = new ApplyExpr(pos, fn, new StringLiteral(pos, "hello"), nilOp);
		JSString s = new JSString("s");
		JSExpr nil = context.mock(JSExpr.class, "nil");
		JSExpr cons = context.mock(JSExpr.class, "cons");
		context.checking(new Expectations() {{
			oneOf(meth).string("hello"); will(returnValue(s));
			oneOf(meth).structConst(new SolidName(LoadBuiltins.builtinPkg, "Nil")); will(returnValue(nil));
			oneOf(meth).structArgs(new SolidName(LoadBuiltins.builtinPkg, "Cons"), s, nil); will(returnValue(cons));
			oneOf(nv).result(cons);
		}});
		StackVisitor stackv = new StackVisitor();
		stackv.push(nv);
		new ExprGeneratorJS(state, stackv, meth, false);
		Traverser gen = new Traverser(stackv).withHSI();
		gen.visitApplyExpr(ae);
	}
	
	@Test
	public void aConstructorApplicationWithInsufficientArgsBecomesACurry() {
		UnresolvedVar fn = new UnresolvedVar(pos, "Cons");
		fn.bind(LoadBuiltins.cons);
		ApplyExpr ae = new ApplyExpr(pos, fn, new StringLiteral(pos, "hello"));
		JSString s = new JSString("s");
		JSExpr cons = context.mock(JSExpr.class, "cons");
		JSExpr curry = context.mock(JSExpr.class, "curry");
		context.checking(new Expectations() {{
			oneOf(meth).pushConstructor(new SolidName(LoadBuiltins.builtinPkg, "Cons"), "Cons"); will(returnValue(cons));
			oneOf(meth).string("hello"); will(returnValue(s));
			oneOf(meth).curry(false, 2, cons, s); will(returnValue(curry));
			oneOf(nv).result(curry);
		}});
		StackVisitor stackv = new StackVisitor();
		stackv.push(nv);
		new ExprGeneratorJS(state, stackv, meth, false);
		Traverser gen = new Traverser(stackv).withHSI();
		gen.visitApplyExpr(ae);
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void aFunctionApplicationWithExplicitCurrying() {
		UnresolvedVar fn = new UnresolvedVar(pos, "f");
		FunctionName fnName = FunctionName.function(pos, pkg, "f");
		fn.bind(new FunctionDefinition(fnName, 2, null));
		AnonymousVar uv = new AnonymousVar(pos);
		ApplyExpr ae = new ApplyExpr(pos, fn, uv, new StringLiteral(pos, "hello"));
		JSExpr f = context.mock(JSExpr.class, "f");
		JSString sv = new JSString("s");
		context.checking(new Expectations() {{
			oneOf(meth).pushFunction("test__repo.f", fnName, 2); will(returnValue(f));
			oneOf(meth).string("hello"); will(returnValue(sv));
			oneOf(meth).xcurry(with(false), with(2), (List<XCArg>) with(Matchers.contains(Matchers.equalTo(new XCArg(0, f)), Matchers.equalTo(new XCArg(2, sv))))); will(returnValue(res));
			oneOf(nv).result(res);
		}});
		StackVisitor stackv = new StackVisitor();
		stackv.push(nv);
		new ExprGeneratorJS(state, stackv, meth, false);
		Traverser gen = new Traverser(stackv).withHSI();
		gen.visitApplyExpr(ae);
	}

	@Test
	public void errorsWantToBeCreatedThroughTheContext() {
		StringLiteral lit = new StringLiteral(pos, "error message");
		UnresolvedVar err = new UnresolvedVar(pos, "Error");
		StructDefn errT = new StructDefn(pos, pos, FieldsType.STRUCT, new SolidName(LoadBuiltins.builtinPkg, "Error"), false, new ArrayList<>());
		errT.addField(new StructField(pos, errT, false, true, LoadBuiltins.stringTR, "msg"));
		err.bind(errT);
		ApplyExpr ae = new ApplyExpr(pos, err, lit);
		JSString s = new JSString("s");
		JSExpr errjs = context.mock(JSExpr.class, "errjs");
		context.checking(new Expectations() {{
			oneOf(meth).string("error message"); will(returnValue(s));
			oneOf(meth).structArgs(new SolidName(LoadBuiltins.builtinPkg, "FLError"), s); will(returnValue(errjs));
			oneOf(nv).result(errjs);
		}});
		StackVisitor stackv = new StackVisitor();
		stackv.push(nv);
		new ExprGeneratorJS(state, stackv, meth, false);
		Traverser gen = new Traverser(stackv);
		gen.visitApplyExpr(ae);
	}
	
	@Test
	public void aSpuriousApplyExprIsIgnored() {
		NumericLiteral nl = new NumericLiteral(pos, "42", 2);
		ApplyExpr expr = new ApplyExpr(pos, nl);
		context.checking(new Expectations() {{
			oneOf(meth).literal("42"); will(returnValue(res));
			oneOf(nv).result(res);
		}});
		StackVisitor stackv = new StackVisitor();
		stackv.push(nv);
		Traverser gen = new Traverser(new ExprGeneratorJS(state, stackv, meth, false));
		gen.visitExpr(expr, 0);
	}
	
	@Test
	public void anEmptyListIsNil() {
		UnresolvedOperator op = new UnresolvedOperator(pos, "[]");
		op.bind(LoadBuiltins.nil);
		ApplyExpr expr = new ApplyExpr(pos, op);
		JSExpr nil = context.mock(JSExpr.class, "nil");
		context.checking(new Expectations() {{
			oneOf(meth).structConst(new SolidName(LoadBuiltins.builtinPkg, "Nil")); will(returnValue(nil));
			oneOf(nv).result(nil);
		}});
		StackVisitor stackv = new StackVisitor();
		stackv.push(nv);
		Traverser gen = new Traverser(new ExprGeneratorJS(state, stackv, meth, false));
		gen.visitExpr(expr, 0);
	}
	
	@Test
	public void aSingletonListUsesMakeArray() {
		UnresolvedOperator op = new UnresolvedOperator(pos, "[]");
		op.bind(LoadBuiltins.nil);
		NumericLiteral nl = new NumericLiteral(pos, "42", 2);
		ApplyExpr expr = new ApplyExpr(pos, op, nl);
		JSExpr lit = context.mock(JSExpr.class, "lit");
		List<JSExpr> alit = new ArrayList<>();
		alit.add(lit);
		JSExpr ma = context.mock(JSExpr.class, "ma");
		context.checking(new Expectations() {{
			oneOf(meth).literal("42"); will(returnValue(lit));
			oneOf(meth).makeArray(alit); will(returnValue(ma));
			oneOf(nv).result(ma);
		}});
		StackVisitor stackv = new StackVisitor();
		stackv.push(nv);
		new ExprGeneratorJS(state, stackv, meth, false);
		Traverser gen = new Traverser(stackv).withHSI();
		gen.visitApplyExpr(expr);
	}

	@Test
	public void messagesPushesNestedExprGeneratorAndThenCollectsAList() {
		NumericLiteral msg1 = new NumericLiteral(pos, "42", 2);
		NumericLiteral msg2 = new NumericLiteral(pos, "84", 2);
		Messages me = new Messages(pos, Arrays.asList(msg1, msg2));
		StackVisitor stackv = new StackVisitor();
		stackv.push(nv);
		JSExpr li1 = context.mock(JSExpr.class, "li1");
		JSExpr li2 = context.mock(JSExpr.class, "li2");
		JSExpr msgs = context.mock(JSExpr.class, "msgs");
		context.checking(new Expectations() {{
			oneOf(meth).literal("42"); will(returnValue(li1));
			oneOf(meth).literal("84"); will(returnValue(li2));
			oneOf(meth).makeArray(li1, li2); will(returnValue(msgs));
			oneOf(nv).result(msgs);
		}});
		new ExprGeneratorJS(state, stackv, meth, false);
		Traverser gen = new Traverser(stackv).withHSI();
		gen.visitMessages(me);
	}

	@Test
	public void aDotOperator() {
		UnresolvedVar from = new UnresolvedVar(pos, "from");
		UnresolvedVar fld = new UnresolvedVar(pos, "fld");
		MemberExpr me = new MemberExpr(pos, from, fld);
		TypeReference ctr = new TypeReference(pos, "Ctr");
		ContractDecl cd = new ContractDecl(pos, pos, ContractType.CONTRACT, new SolidName(pkg, "Ctr"));
		ctr.bind(cd);
		TypedPattern tp = new TypedPattern(pos, ctr, new VarName(pos, cd.name(), "from"));
		from.bind(tp);
		MakeSend ms = new MakeSend(pos, FunctionName.contractMethod(pos, cd.name(), "f"), from, 0);
		me.conversion(ms);
		StackVisitor stackv = new StackVisitor();
		stackv.push(nv);
		JSExpr fv = context.mock(JSExpr.class, "fv");
		context.checking(new Expectations() {{
			oneOf(meth).boundVar("from"); will(returnValue(fv));
			oneOf(meth).makeSend("f", fv, 0, null, null);
		}});
		new ApplyExprGeneratorJS(state, stackv, meth);
		Traverser gen = new Traverser(stackv).withHSI();
		gen.visitExpr(me, 0);
	}
	
	@Test
	public void applyingADotOperator() {
		UnresolvedVar from = new UnresolvedVar(pos, "from");
		UnresolvedVar fld = new UnresolvedVar(pos, "fld");
		MemberExpr me = new MemberExpr(pos, from, fld);
		TypeReference ctr = new TypeReference(pos, "Ctr");
		ContractDecl cd = new ContractDecl(pos, pos, ContractType.CONTRACT, new SolidName(pkg, "Ctr"));
		ctr.bind(cd);
		TypedPattern tp = new TypedPattern(pos, ctr, new VarName(pos, cd.name(), "from"));
		from.bind(tp);
		MakeSend ms = new MakeSend(pos, FunctionName.contractMethod(pos, cd.name(), "f"), from, 2);
		me.conversion(ms);
		ApplyExpr ae = new ApplyExpr(pos, me, new NumericLiteral(pos, "42", 2), new StringLiteral(pos, "hello"));
		StackVisitor stackv = new StackVisitor();
		stackv.push(nv);
		JSExpr fv = context.mock(JSExpr.class, "fv");
		JSExpr msi = context.mock(JSExpr.class, "msi");
		JSExpr n1 = context.mock(JSExpr.class, "n1");
		JSString s1 = new JSString("s");
		context.checking(new Expectations() {{
			oneOf(meth).boundVar("from"); will(returnValue(fv));
			oneOf(meth).makeSend("f", fv, 2, null, null); will(returnValue(msi));
			oneOf(meth).literal("42"); will(returnValue(n1));
			oneOf(meth).string("hello"); will(returnValue(s1));
			oneOf(meth).closure(false, msi, n1, s1); will(returnValue(res));
			oneOf(nv).result(res);
		}});
		new ExprGeneratorJS(state, stackv, meth, false);
		Traverser gen = new Traverser(stackv).withHSI();
		gen.visitApplyExpr(ae);
	}
	
	@Test
	public void aDotOperatorAsAnAccessor() {
		UnresolvedVar from = new UnresolvedVar(pos, "from");
		UnresolvedVar fld = new UnresolvedVar(pos, "fld");
		MemberExpr me = new MemberExpr(pos, from, fld);
		TypeReference obj = new TypeReference(pos, "Obj");
		ObjectDefn od = new ObjectDefn(pos, pos, new SolidName(pkg, "Obj"), true, new ArrayList<>());
		obj.bind(od);
		TypedPattern tp = new TypedPattern(pos, obj, new VarName(pos, od.name(), "from"));
		from.bind(tp);
		MakeAcor ma = new MakeAcor(pos, FunctionName.contractMethod(pos, od.name(), "f"), from, 0);
		me.conversion(ma);
		StackVisitor stackv = new StackVisitor();
		stackv.push(nv);
		JSExpr fv = context.mock(JSExpr.class, "fv");
		context.checking(new Expectations() {{
			oneOf(meth).boundVar("from"); will(returnValue(fv));
			oneOf(meth).makeAcor((FunctionName) ma.name(), fv, 0);
		}});
		new ApplyExprGeneratorJS(state, stackv, meth);
		Traverser gen = new Traverser(stackv).withHSI();
		gen.visitExpr(me, 0);
	}
	
	
	@Test
	public void aDotOperatorAsAnAccessorWithArguments() {
		UnresolvedVar from = new UnresolvedVar(pos, "from");
		UnresolvedVar fld = new UnresolvedVar(pos, "fld");
		MemberExpr me = new MemberExpr(pos, from, fld);
		TypeReference obj = new TypeReference(pos, "Obj");
		ObjectDefn od = new ObjectDefn(pos, pos, new SolidName(pkg, "Obj"), true, new ArrayList<>());
		obj.bind(od);
		TypedPattern tp = new TypedPattern(pos, obj, new VarName(pos, od.name(), "from"));
		from.bind(tp);
		MakeAcor ma = new MakeAcor(pos, FunctionName.contractMethod(pos, od.name(), "f"), from, 2);
		me.conversion(ma);
		ApplyExpr ae = new ApplyExpr(pos, me, new NumericLiteral(pos, "42", 2), new StringLiteral(pos, "hello"));
		StackVisitor stackv = new StackVisitor();
		stackv.push(nv);
		JSExpr fv = context.mock(JSExpr.class, "fv");
		JSExpr msi = context.mock(JSExpr.class, "msi");
		JSExpr n1 = context.mock(JSExpr.class, "n1");
		JSString s1 = new JSString("s");
		context.checking(new Expectations() {{
			oneOf(meth).boundVar("from"); will(returnValue(fv));
			oneOf(meth).makeAcor((FunctionName) ma.name(), fv, 2); will(returnValue(msi));
			oneOf(meth).literal("42"); will(returnValue(n1));
			oneOf(meth).string("hello"); will(returnValue(s1));
			oneOf(meth).closure(false, msi, n1, s1); will(returnValue(res));
			oneOf(nv).result(res);
		}});
		new ExprGeneratorJS(state, stackv, meth, false);
		Traverser gen = new Traverser(stackv).withHSI();
		gen.visitExpr(ae, 0);
	}
}
