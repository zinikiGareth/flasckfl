package test.flas.generator.js;

import java.util.ArrayList;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.ApplyExpr;
import org.flasck.flas.commonBase.NumericLiteral;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.compiler.jsgen.JSExpr;
import org.flasck.flas.compiler.jsgen.JSGenerator;
import org.flasck.flas.compiler.jsgen.JSMethodCreator;
import org.flasck.flas.compiler.jsgen.JSStorage;
import org.flasck.flas.parsedForm.FieldsDefn.FieldsType;
import org.flasck.flas.parsedForm.FunctionCaseDefn;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.StructField;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.parsedForm.UnresolvedOperator;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.repository.Traverser;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Rule;
import org.junit.Test;

public class ExpressionGenerationJS {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	private JSMethodCreator meth = context.mock(JSMethodCreator.class);
	private InputPosition pos = new InputPosition("-", 1, 0, null);
	private final PackageName pkg = new PackageName("test.repo");

	@Test
	public void aSimpleInteger() {
		context.checking(new Expectations() {{
			oneOf(meth).literal("42");
		}});
		NumericLiteral expr = new NumericLiteral(pos, "42", 2);
		Traverser gen = new Traverser(JSGenerator.forTests(meth , null));
		gen.visitExpr(expr, 0);
	}

	@Test
	public void aSimpleString() {
		context.checking(new Expectations() {{
			oneOf(meth).string("hello");
		}});
		StringLiteral expr = new StringLiteral(pos, "hello");
		Traverser gen = new Traverser(JSGenerator.forTests(meth, null));
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
		Traverser gen = new Traverser(JSGenerator.forTests(meth, null));
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
		Traverser gen = new Traverser(JSGenerator.forTests(meth, null));
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
		Traverser gen = new Traverser(JSGenerator.forTests(meth, null));
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
		Traverser gen = new Traverser(JSGenerator.forTests(meth, null));
		gen.visitExpr(expr, 0);
	}

	@Test
	public void aSimpleFunction() {
		JSStorage jss = context.mock(JSStorage.class);
		JSExpr nret = context.mock(JSExpr.class, "nret");
		context.checking(new Expectations() {{
			oneOf(jss).newFunction("test.repo", "x"); will(returnValue(meth));
			oneOf(meth).structConst("test.repo.Ctor"); will(returnValue(nret));
			oneOf(meth).returnObject(nret);
		}});
		JSGenerator gen = new JSGenerator(jss);
		FunctionName name = FunctionName.function(pos, pkg, "x");
		FunctionDefinition fn = new FunctionDefinition(name, 0);
		FunctionIntro fi = new FunctionIntro(name, new ArrayList<>());
		UnresolvedVar expr = new UnresolvedVar(pos, "Ctor");
		expr.bind(new StructDefn(pos, FieldsType.STRUCT, "test.repo", "Ctor", true));
		FunctionCaseDefn fcd = new FunctionCaseDefn(null, expr);
		fi.functionCase(fcd);
		fn.intro(fi);
		new Traverser(gen).visitFunction(fn);
	}

	@Test
	public void anOp() {
		context.checking(new Expectations() {{
			oneOf(meth).pushFunction("FLEval.plus");
		}});
		UnresolvedOperator expr = new UnresolvedOperator(pos, "+");
		Traverser gen = new Traverser(JSGenerator.forTests(meth, null));
		gen.visitExpr(expr, 2);
	}

	@Test
	public void aFunctionApplication() {
		UnresolvedVar fn = new UnresolvedVar(pos, "f");
		FunctionName fnName = FunctionName.function(pos, pkg, "f");
		fn.bind(new FunctionDefinition(fnName, 2));
		ApplyExpr ae = new ApplyExpr(pos, fn, new NumericLiteral(pos, "42", 2), new StringLiteral(pos, "hello"));
		JSExpr f = context.mock(JSExpr.class, "f");
		JSExpr nv = context.mock(JSExpr.class, "nv");
		JSExpr sv = context.mock(JSExpr.class, "sv");
		context.checking(new Expectations() {{
			oneOf(meth).pushFunction("test.repo.f"); will(returnValue(f));
			oneOf(meth).literal("42"); will(returnValue(nv));
			oneOf(meth).string("hello"); will(returnValue(sv));
			oneOf(meth).closure(f, nv, sv);
		}});
		Traverser gen = new Traverser(JSGenerator.forTests(meth, null));
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
		context.checking(new Expectations() {{
			oneOf(meth).pushFunction("test.repo.x"); will(returnValue(x));
			oneOf(meth).closure(x); will(returnValue(v1));
			oneOf(meth).pushFunction("test.repo.f"); will(returnValue(f));
			oneOf(meth).closure(f, v1);
		}});
		Traverser gen = new Traverser(JSGenerator.forTests(meth, null));
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
		Traverser gen = new Traverser(JSGenerator.forTests(meth, null));
		gen.visitExpr(ae, 0);
	}
	
	@Test
	public void aSpuriousApplyExprIsIgnored() {
		NumericLiteral nl = new NumericLiteral(pos, "42", 2);
		ApplyExpr expr = new ApplyExpr(pos, nl);
		context.checking(new Expectations() {{
			oneOf(meth).literal("42");
		}});
		Traverser gen = new Traverser(JSGenerator.forTests(meth, null));
		gen.visitExpr(expr, 0);
	}
}
