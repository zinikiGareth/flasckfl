package test.flas.generator.js;

import java.util.ArrayList;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.NumericLiteral;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.compiler.jsgen.GuardGeneratorJS;
import org.flasck.flas.compiler.jsgen.JSBlockCreator;
import org.flasck.flas.compiler.jsgen.JSExpr;
import org.flasck.flas.compiler.jsgen.JSIfExpr;
import org.flasck.flas.compiler.jsgen.JSMethodCreator;
import org.flasck.flas.parsedForm.FunctionCaseDefn;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.repository.LoadBuiltins;
import org.flasck.flas.repository.Repository.Visitor;
import org.flasck.flas.repository.StackVisitor;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Rule;
import org.junit.Test;

public class GuardGenerationJS {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	private JSMethodCreator meth = context.mock(JSMethodCreator.class);
	private Visitor v = context.mock(Visitor.class);
	private InputPosition pos = new InputPosition("-", 1, 0, null);
	private final PackageName pkg = new PackageName("test.repo");

	@Test
	public void aSimpleGuard() {
		StackVisitor gen = new StackVisitor();
		gen.push(v);
		gen.push(new GuardGeneratorJS(gen, meth));

		FunctionName name = FunctionName.function(pos, pkg, "x");
		FunctionDefinition fn = new FunctionDefinition(name, 0);
		FunctionIntro fi = new FunctionIntro(name, new ArrayList<>());
		UnresolvedVar t = new UnresolvedVar(pos, "True");
		t.bind(LoadBuiltins.trueT);
		NumericLiteral expr = new NumericLiteral(pos, "42", 2);
		FunctionCaseDefn fcd = new FunctionCaseDefn(t, expr);
		fi.functionCase(fcd);
		fn.intro(fi);

		JSExpr ge = context.mock(JSExpr.class, "ge");
		JSBlockCreator yesGuard = context.mock(JSBlockCreator.class, "yesGuard");
		JSBlockCreator noGuard = context.mock(JSBlockCreator.class, "noGuard");
		JSIfExpr guard = new JSIfExpr(null, yesGuard, noGuard);
		JSExpr r1 = context.mock(JSExpr.class, "r1");

		context.checking(new Expectations() {{
			oneOf(meth).structConst("True"); will(returnValue(ge));
			oneOf(meth).ifTrue(ge); will(returnValue(guard));
			oneOf(yesGuard).literal("42"); will(returnValue(r1));
			oneOf(yesGuard).returnObject(r1);
			oneOf(noGuard).errorNoDefaultGuard();
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
