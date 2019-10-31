package test.flas.generator.js;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.ApplyExpr;
import org.flasck.flas.commonBase.NumericLiteral;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.commonBase.names.VarName;
import org.flasck.flas.compiler.jsgen.ExprGeneratorJS;
import org.flasck.flas.compiler.jsgen.JSExpr;
import org.flasck.flas.compiler.jsgen.JSGenerator;
import org.flasck.flas.compiler.jsgen.JSGenerator.XCArg;
import org.flasck.flas.compiler.jsgen.JSMethodCreator;
import org.flasck.flas.compiler.jsgen.JSStorage;
import org.flasck.flas.parsedForm.FieldsDefn.FieldsType;
import org.flasck.flas.parsedForm.FunctionCaseDefn;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.StructField;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.parsedForm.TypedPattern;
import org.flasck.flas.parsedForm.UnresolvedOperator;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.parsedForm.VarPattern;
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
	private InputPosition pos = new InputPosition("-", 1, 0, null);
	private final PackageName pkg = new PackageName("test.repo");

	@Test
	public void aSimpleInteger() {
		context.checking(new Expectations() {{
			oneOf(meth).literal("42");
		}});
		NumericLiteral expr = new NumericLiteral(pos, "42", 2);
		Traverser gen = new Traverser(new ExprGeneratorJS(nv, meth));
		gen.visitExpr(expr, 0);
	}

	@Test
	public void aSimpleString() {
		context.checking(new Expectations() {{
			oneOf(meth).string("hello");
		}});
		StringLiteral expr = new StringLiteral(pos, "hello");
		Traverser gen = new Traverser(new ExprGeneratorJS(nv, meth));
		gen.visitExpr(expr, 0);
	}

	@Test
	public void aVar() {
		context.checking(new Expectations() {{
			oneOf(meth).pushFunction("test.repo.x");
		}});
		UnresolvedVar expr = new UnresolvedVar(pos, "x");
		FunctionName nameX = FunctionName.function(pos, pkg, "x");
		expr.bind(new FunctionDefinition(nameX, 0));
		Traverser gen = new Traverser(new ExprGeneratorJS(nv, meth));
		gen.visitExpr(expr, 2);
	}
	
	@Test
	public void aFunctionArgument() {
		UnresolvedVar expr = new UnresolvedVar(pos, "p");
		FunctionName nameX = FunctionName.function(pos, pkg, "p");
		expr.bind(new VarPattern(pos, new VarName(pos, nameX, "p")));
		context.checking(new Expectations() {{
			oneOf(meth).boundVar("p");
		}});
		Traverser gen = new Traverser(new ExprGeneratorJS(nv, meth));
		gen.visitExpr(expr, 2);
	}
	
	@Test
	public void aTypedFunctionArgument() {
		UnresolvedVar expr = new UnresolvedVar(pos, "p");
		FunctionName nameX = FunctionName.function(pos, pkg, "p");
		TypeReference string = new TypeReference(pos, "String");
		string.bind(LoadBuiltins.string);
		expr.bind(new TypedPattern(pos, string, new VarName(pos, nameX, "p")));
		context.checking(new Expectations() {{
			oneOf(meth).boundVar("p");
		}});
		Traverser gen = new Traverser(new ExprGeneratorJS(nv, meth));
		gen.visitExpr(expr, 2);
	}
	
	@Test
	public void aVarWithNoArgsExpectingNoArgsBecomesAClosureByItself() {
		JSExpr x = context.mock(JSExpr.class, "f");
		context.checking(new Expectations() {{
			oneOf(meth).pushFunction("test.repo.x"); will(returnValue(x));
			oneOf(meth).closure(x);
		}});
		UnresolvedVar expr = new UnresolvedVar(pos, "x");
		FunctionName nameX = FunctionName.function(pos, pkg, "x");
		expr.bind(new FunctionDefinition(nameX, 0));
		Traverser gen = new Traverser(new ExprGeneratorJS(nv, meth));
		gen.visitExpr(expr, 0);
	}

	@Test
	public void aVarWithNoArgsExpectingTwoArgsBecomesACurriedFunction() {
		JSExpr x = context.mock(JSExpr.class, "f");
		context.checking(new Expectations() {{
			oneOf(meth).pushFunction("test.repo.x"); will(returnValue(x));
			oneOf(meth).curry(2, x);
		}});
		UnresolvedVar expr = new UnresolvedVar(pos, "x");
		FunctionName nameX = FunctionName.function(pos, pkg, "x");
		expr.bind(new FunctionDefinition(nameX, 2));
		Traverser gen = new Traverser(new ExprGeneratorJS(nv, meth));
		gen.visitExpr(expr, 0);
	}

	@Test
	public void aStructConstructorWithNoArgsExpectingNoArgsBecomesAConstant() {
		JSExpr x = context.mock(JSExpr.class, "f");
		context.checking(new Expectations() {{
			oneOf(meth).structConst("test.repo.Ctor"); will(returnValue(x));
		}});
		UnresolvedVar expr = new UnresolvedVar(pos, "Ctor");
		expr.bind(new StructDefn(pos, FieldsType.STRUCT, "test.repo", "Ctor", true));
		Traverser gen = new Traverser(new ExprGeneratorJS(nv, meth));
		gen.visitExpr(expr, 0);
	}

	@Test
	public void aSimpleFunction() {
		JSStorage jss = context.mock(JSStorage.class);
		JSExpr nret = context.mock(JSExpr.class, "nret");
		context.checking(new Expectations() {{
			oneOf(jss).newFunction("test.repo", "x"); will(returnValue(meth));
			oneOf(meth).argument("_cxt");
			oneOf(meth).structConst("test.repo.Ctor"); will(returnValue(nret));
			oneOf(meth).returnObject(nret);
		}});
		StackVisitor sv = new StackVisitor();
		new JSGenerator(jss, sv);
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
		new Traverser(sv).withHSI().visitFunction(fn);
	}

	@Test
	public void anOp() {
		context.checking(new Expectations() {{
			oneOf(meth).pushFunction("FLBuiltin.plus");
		}});
		UnresolvedOperator expr = new UnresolvedOperator(pos, "+");
		expr.bind(new FunctionDefinition(FunctionName.function(pos, null, "+"), 2));
		Traverser gen = new Traverser(new ExprGeneratorJS(nv, meth));
		gen.visitExpr(expr, 2);
	}

	@Test
	public void aFunctionApplication() {
		UnresolvedVar fn = new UnresolvedVar(pos, "f");
		FunctionName fnName = FunctionName.function(pos, pkg, "f");
		fn.bind(new FunctionDefinition(fnName, 2));
		ApplyExpr ae = new ApplyExpr(pos, fn, new NumericLiteral(pos, "42", 2), new StringLiteral(pos, "hello"));
		JSExpr f = context.mock(JSExpr.class, "f");
		JSExpr iv = context.mock(JSExpr.class, "iv");
		JSExpr sv = context.mock(JSExpr.class, "sv");
		StackVisitor stackv = new StackVisitor();
		stackv.push(nv);
		context.checking(new Expectations() {{
			oneOf(meth).pushFunction("test.repo.f"); will(returnValue(f));
			oneOf(meth).literal("42"); will(returnValue(iv));
			oneOf(meth).string("hello"); will(returnValue(sv));
			oneOf(meth).closure(f, iv, sv);
		}});
		Traverser gen = new Traverser(new ExprGeneratorJS(stackv, meth));
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
		JSExpr f = context.mock(JSExpr.class, "f");
		JSExpr x = context.mock(JSExpr.class, "x");
		JSExpr v1 = context.mock(JSExpr.class, "v1");
		StackVisitor stackv = new StackVisitor();
		stackv.push(nv);
		context.checking(new Expectations() {{
			oneOf(meth).pushFunction("test.repo.x"); will(returnValue(x));
			oneOf(meth).closure(x); will(returnValue(v1));
			oneOf(meth).pushFunction("test.repo.f"); will(returnValue(f));
			oneOf(meth).closure(f, v1);
		}});
		Traverser gen = new Traverser(new ExprGeneratorJS(stackv, meth));
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
		ApplyExpr ae = new ApplyExpr(pos, fn, new StringLiteral(pos, "hello"), nilOp);
		JSExpr s = context.mock(JSExpr.class, "s");
		JSExpr nil = context.mock(JSExpr.class, "nil");
		JSExpr cons = context.mock(JSExpr.class, "cons");
		context.checking(new Expectations() {{
			oneOf(meth).string("hello"); will(returnValue(s));
			oneOf(meth).structConst("Nil"); will(returnValue(nil));
			oneOf(meth).callFunction("Cons", s, nil); will(returnValue(cons));
		}});
		StackVisitor stackv = new StackVisitor();
		stackv.push(nv);
		Traverser gen = new Traverser(new ExprGeneratorJS(stackv, meth));
		gen.visitExpr(ae, 0);
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void aFunctionApplicationWithExplicitCurrying() {
		UnresolvedVar fn = new UnresolvedVar(pos, "f");
		FunctionName fnName = FunctionName.function(pos, pkg, "f");
		fn.bind(new FunctionDefinition(fnName, 2));
		UnresolvedVar uv = new UnresolvedVar(pos, "_");
		uv.bind(LoadBuiltins.ca);
		ApplyExpr ae = new ApplyExpr(pos, fn, uv, new StringLiteral(pos, "hello"));
		JSExpr f = context.mock(JSExpr.class, "f");
		JSExpr sv = context.mock(JSExpr.class, "sv");
		context.checking(new Expectations() {{
			oneOf(meth).pushFunction("test.repo.f"); will(returnValue(f));
			oneOf(meth).string("hello"); will(returnValue(sv));
			oneOf(meth).xcurry(with(2), (List<XCArg>) with(Matchers.contains(Matchers.equalTo(new XCArg(0, f)), Matchers.equalTo(new XCArg(2, sv)))));
		}});
		StackVisitor stackv = new StackVisitor();
		stackv.push(nv);
		Traverser gen = new Traverser(new ExprGeneratorJS(stackv, meth));
		gen.visitExpr(ae, 0);
	}

	@Test
	public void errorsWantToBeCreatedThroughTheContext() {
		StringLiteral lit = new StringLiteral(pos, "error message");
		UnresolvedVar err = new UnresolvedVar(pos, "Error");
		StructDefn errT = new StructDefn(pos, FieldsType.STRUCT, null, "Error", false);
		err.bind(errT);
		ApplyExpr ae = new ApplyExpr(pos, err, lit);
		JSExpr s = context.mock(JSExpr.class, "s");
		JSExpr errjs = context.mock(JSExpr.class, "cons");
		context.checking(new Expectations() {{
			oneOf(meth).string("error message"); will(returnValue(s));
			oneOf(meth).callFunction("FLError", s); will(returnValue(errjs));
		}});
		StackVisitor stackv = new StackVisitor();
		stackv.push(nv);
		Traverser gen = new Traverser(new ExprGeneratorJS(stackv, meth));
		gen.visitExpr(ae, 0);
	}
	
	@Test
	public void aSpuriousApplyExprIsIgnored() {
		NumericLiteral nl = new NumericLiteral(pos, "42", 2);
		ApplyExpr expr = new ApplyExpr(pos, nl);
		context.checking(new Expectations() {{
			oneOf(meth).literal("42");
		}});
		StackVisitor stackv = new StackVisitor();
		stackv.push(nv);
		Traverser gen = new Traverser(new ExprGeneratorJS(stackv, meth));
		gen.visitExpr(expr, 0);
	}
	
	@Test
	public void anEmptyListIsNil() {
		UnresolvedOperator op = new UnresolvedOperator(pos, "[]");
		op.bind(new FunctionDefinition(FunctionName.function(pos, null, "[]"), 0));
		ApplyExpr expr = new ApplyExpr(pos, op);
		JSExpr nil = context.mock(JSExpr.class, "nil");
		JSExpr ex = context.mock(JSExpr.class, "expr");
		context.checking(new Expectations() {{
			oneOf(meth).pushFunction("Nil"); will(returnValue(nil));
			oneOf(meth).closure(nil); will(returnValue(ex));
		}});
		StackVisitor stackv = new StackVisitor();
		stackv.push(nv);
		Traverser gen = new Traverser(new ExprGeneratorJS(stackv, meth));
		gen.visitExpr(expr, 0);
	}
	
	@Test
	public void aSingletonListUsesMakeArray() {
		UnresolvedOperator op = new UnresolvedOperator(pos, "[]");
		op.bind(new FunctionDefinition(FunctionName.function(pos, null, "[]"), 0));
		NumericLiteral nl = new NumericLiteral(pos, "42", 2);
		ApplyExpr expr = new ApplyExpr(pos, op, nl);
		JSExpr maop = context.mock(JSExpr.class, "maop");
		JSExpr lit = context.mock(JSExpr.class, "lit");
		JSExpr ma = context.mock(JSExpr.class, "ma");
		context.checking(new Expectations() {{
			oneOf(meth).pushFunction("MakeArray"); will(returnValue(maop));
			oneOf(meth).literal("42"); will(returnValue(lit));
			oneOf(meth).closure(maop, lit); will(returnValue(ma));
		}});
		StackVisitor stackv = new StackVisitor();
		stackv.push(nv);
		Traverser gen = new Traverser(new ExprGeneratorJS(stackv, meth));
		gen.visitExpr(expr, 0);
	}
}
