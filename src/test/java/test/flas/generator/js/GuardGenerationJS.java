package test.flas.generator.js;

import java.util.ArrayList;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.commonBase.NumericLiteral;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.compiler.jsgen.ExprGeneratorJS;
import org.flasck.flas.compiler.jsgen.JSMethodCreator;
import org.flasck.flas.parsedForm.FunctionCaseDefn;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.repository.Repository.Visitor;
import org.flasck.flas.repository.LoadBuiltins;
import org.flasck.flas.repository.StackVisitor;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

public class GuardGenerationJS {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	private JSMethodCreator meth = context.mock(JSMethodCreator.class);
	private Visitor nv = context.mock(Visitor.class);
	private InputPosition pos = new InputPosition("-", 1, 0, null);
	private final PackageName pkg = new PackageName("test.repo");

	@Test
	@Ignore
	public void aSimpleGuard() {
		StackVisitor gen = new StackVisitor();
		gen.push(nv);
		gen.push(new ExprGeneratorJS(gen, meth));

		FunctionName name = FunctionName.function(pos, pkg, "x");
		FunctionDefinition fn = new FunctionDefinition(name, 0);
		FunctionIntro fi = new FunctionIntro(name, new ArrayList<>());
		UnresolvedVar t = new UnresolvedVar(pos, "True");
		t.bind(LoadBuiltins.trueT);
		NumericLiteral expr = new NumericLiteral(pos, "42", 2);
		FunctionCaseDefn fcd = new FunctionCaseDefn(t, expr);
		fi.functionCase(fcd);
		fn.intro(fi);

		context.checking(new Expectations() {{
			oneOf(meth).structConst("True");
			oneOf(meth).literal("42");
		}});
		
		gen.startInline(fi);
		gen.visitCase(fcd);
		gen.visitGuard(fcd);
		gen.visitExpr(t, 0);
		gen.visitUnresolvedVar(t, 0);
		gen.leaveGuard(fcd);
		gen.visitExpr(expr, 0);
		gen.visitNumericLiteral(expr);
		gen.leaveCase(fcd);
		gen.endInline(fi);
	}

}
