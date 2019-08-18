package test.flas.generator.js;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.ApplyExpr;
import org.flasck.flas.commonBase.NumericLiteral;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.compiler.jsgen.JSExpr;
import org.flasck.flas.compiler.jsgen.JSGenerator;
import org.flasck.flas.compiler.jsgen.JSMethodCreator;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.UnresolvedOperator;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.parsedForm.FieldsDefn.FieldsType;
import org.flasck.flas.repository.Traverser;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Rule;
import org.junit.Test;

public class ExpressionGenerationJS {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	private InputPosition pos = new InputPosition("-", 1, 0, null);
	private final PackageName pkg = new PackageName("test.repo");

	@Test
	public void aSimpleInteger() {
		JSMethodCreator meth = context.mock(JSMethodCreator.class);
		context.checking(new Expectations() {{
			oneOf(meth).literal("42");
		}});
		NumericLiteral expr = new NumericLiteral(pos, "42", 2);
		Traverser gen = new Traverser(JSGenerator.forTests(meth , null));
		gen.visitExpr(expr, 0);
	}

	@Test
	public void aSimpleString() {
		JSMethodCreator meth = context.mock(JSMethodCreator.class);
		context.checking(new Expectations() {{
			oneOf(meth).string("hello");
		}});
		StringLiteral expr = new StringLiteral(pos, "hello");
		Traverser gen = new Traverser(JSGenerator.forTests(meth, null));
		gen.visitExpr(expr, 0);
	}

	@Test
	public void aVar() {
		JSMethodCreator meth = context.mock(JSMethodCreator.class);
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
		JSMethodCreator meth = context.mock(JSMethodCreator.class);
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
	public void aStructConstructorWithNoArgsExpectingNoArgsBecomesAConstant() {
		JSMethodCreator meth = context.mock(JSMethodCreator.class);
		JSExpr x = context.mock(JSExpr.class, "f");
		context.checking(new Expectations() {{
			oneOf(meth).callFunction("test.repo.Ctor"); will(returnValue(x));
		}});
		UnresolvedVar expr = new UnresolvedVar(pos, "Ctor");
		expr.bind(new StructDefn(pos, FieldsType.STRUCT, "test.repo", "Ctor", true));
		Traverser gen = new Traverser(JSGenerator.forTests(meth, null));
		gen.visitExpr(expr, 0);
	}

	@Test
	public void anOp() {
		JSMethodCreator meth = context.mock(JSMethodCreator.class);
		context.checking(new Expectations() {{
			oneOf(meth).pushFunction("FLEval.plus");
		}});
		UnresolvedOperator expr = new UnresolvedOperator(pos, "+");
		Traverser gen = new Traverser(JSGenerator.forTests(meth, null));
		gen.visitExpr(expr, 2);
	}

	@Test
	public void aFunctionApplication() {
		JSMethodCreator meth = context.mock(JSMethodCreator.class);
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
}
