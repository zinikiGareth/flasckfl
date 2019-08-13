package test.flas.generator.js;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.NumericLiteral;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.compiler.jsgen.JSGenerator;
import org.flasck.flas.compiler.jsgen.JSMethodCreator;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.UnresolvedVar;
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
		gen.visitExpr(expr);
	}

	@Test
	public void aSimpleString() {
		JSMethodCreator meth = context.mock(JSMethodCreator.class);
		context.checking(new Expectations() {{
			oneOf(meth).string("hello");
		}});
		StringLiteral expr = new StringLiteral(pos, "hello");
		Traverser gen = new Traverser(JSGenerator.forTests(meth, null));
		gen.visitExpr(expr);
	}

	@Test
	public void aVar() {
		JSMethodCreator meth = context.mock(JSMethodCreator.class);
		context.checking(new Expectations() {{
			oneOf(meth).callStatic("test.repo.x", "eval");
		}});
		UnresolvedVar expr = new UnresolvedVar(pos, "x");
		FunctionName nameX = FunctionName.function(pos, pkg, "x");
		expr.bind(new FunctionDefinition(nameX, 0));
		Traverser gen = new Traverser(JSGenerator.forTests(meth, null));
		gen.visitExpr(expr);
	}
}
