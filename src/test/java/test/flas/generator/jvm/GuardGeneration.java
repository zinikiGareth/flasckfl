package test.flas.generator.jvm;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.NumericLiteral;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.compiler.jvmgen.FunctionState;
import org.flasck.flas.compiler.jvmgen.GuardGenerator;
import org.flasck.flas.parsedForm.FunctionCaseDefn;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.repository.LoadBuiltins;
import org.flasck.flas.repository.Repository.Visitor;
import org.flasck.jvm.J;
import org.hamcrest.Matchers;
import org.flasck.flas.repository.StackVisitor;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Rule;
import org.junit.Test;
import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.JavaType;
import org.zinutils.bytecode.MethodDefiner;

public class GuardGeneration {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	private final MethodDefiner meth = context.mock(MethodDefiner.class);
	private final IExpr fcx = context.mock(IExpr.class, "fcx");
	private Visitor v = context.mock(Visitor.class);
	private InputPosition pos = new InputPosition("-", 1, 0, null);
	private final PackageName pkg = new PackageName("test.repo");
	@SuppressWarnings("unchecked")
	private final List<IExpr> block = context.mock(List.class);

	@Test
	public void aSingleGuard() {
		StackVisitor gen = new StackVisitor();
		gen.push(v);
		gen.push(new GuardGenerator(new FunctionState(meth, fcx, null), gen, block));

		FunctionName name = FunctionName.function(pos, pkg, "x");
		FunctionDefinition fn = new FunctionDefinition(name, 0);
		FunctionIntro fi = new FunctionIntro(name, new ArrayList<>());
		UnresolvedVar t = new UnresolvedVar(pos, "True");
		t.bind(LoadBuiltins.trueT);
		StringLiteral expr = new StringLiteral(pos, "hello");
		FunctionCaseDefn fcd = new FunctionCaseDefn(t, expr);
		fi.functionCase(fcd);
		fn.intro(fi);

		IExpr a1 = context.mock(IExpr.class, "a1");
		IExpr t1 = context.mock(IExpr.class, "t1");
		IExpr ro1 = context.mock(IExpr.class, "ro1");
		IExpr e1 = context.mock(IExpr.class, "e1");
		IExpr ist = context.mock(IExpr.class, "ist");
		IExpr s = context.mock(IExpr.class, "s");
		IExpr a2 = context.mock(IExpr.class, "a2");
		IExpr err = context.mock(IExpr.class, "err");
		IExpr rerr = context.mock(IExpr.class, "rerr");

		context.checking(new Expectations() {{
			oneOf(meth).arrayOf(J.OBJECT, new ArrayList<>()); will(returnValue(a1));
			oneOf(meth).callStatic("org.flasck.jvm.builtin.True", J.OBJECT, "eval", fcx, a1); will(returnValue(t1));
			oneOf(meth).stringConst("hello"); will(returnValue(e1));
			oneOf(meth).returnObject(e1); will(returnValue(ro1));
			oneOf(meth).stringConst("no default guard"); will(returnValue(s));
			oneOf(meth).arrayOf(J.OBJECT, s); will(returnValue(a2));
			oneOf(meth).callStatic("org.flasck.jvm.builtin.Error", J.OBJECT, "eval", fcx, a2); will(returnValue(err));
			oneOf(meth).returnObject(err); will(returnValue(rerr));
			oneOf(meth).callStatic(J.FLEVAL, JavaType.boolean_, "isTruthy", fcx, t1); will(returnValue(ist));
			oneOf(meth).ifBoolean(ist, ro1, rerr);
		}});
		
		gen.startInline(fi);
		gen.visitCase(fcd);
		gen.visitGuard(fcd);
		gen.visitExpr(t, 0);
		gen.visitUnresolvedVar(t, 0);
		gen.leaveGuard(fcd);
		gen.visitExpr(expr, 0);
		gen.visitStringLiteral(expr);
		gen.leaveCase(fcd);
		gen.endInline(fi);
	}

	@Test
	public void aSingleGuardWithDefault() {
		StackVisitor gen = new StackVisitor();
		gen.push(v);
		gen.push(new GuardGenerator(new FunctionState(meth, fcx, null), gen, block));

		FunctionName name = FunctionName.function(pos, pkg, "x");
		FunctionDefinition fn = new FunctionDefinition(name, 0);
		FunctionIntro fi = new FunctionIntro(name, new ArrayList<>());
		UnresolvedVar t = new UnresolvedVar(pos, "True");
		t.bind(LoadBuiltins.trueT);
		StringLiteral expr = new StringLiteral(pos, "hello");
		FunctionCaseDefn fcd1 = new FunctionCaseDefn(t, expr);
		fi.functionCase(fcd1);
		StringLiteral expr2 = new StringLiteral(pos, "goodbye");
		FunctionCaseDefn fcd2 = new FunctionCaseDefn(t, expr2);
		fi.functionCase(fcd2);
		fn.intro(fi);

		IExpr a1 = context.mock(IExpr.class, "a1");
		IExpr t1 = context.mock(IExpr.class, "t1");
		IExpr ro1 = context.mock(IExpr.class, "ro1");
		IExpr e1 = context.mock(IExpr.class, "e1");
		IExpr ist = context.mock(IExpr.class, "ist");
		IExpr s = context.mock(IExpr.class, "s");
		IExpr a2 = context.mock(IExpr.class, "a2");
		IExpr err = context.mock(IExpr.class, "err");
		IExpr rerr = context.mock(IExpr.class, "rerr");
		IExpr e2 = context.mock(IExpr.class, "e2");
		IExpr ro2 = context.mock(IExpr.class, "ro2");

		context.checking(new Expectations() {{
			oneOf(meth).arrayOf(J.OBJECT, new ArrayList<>()); will(returnValue(a1));
			oneOf(meth).callStatic("org.flasck.jvm.builtin.True", J.OBJECT, "eval", fcx, a1); will(returnValue(t1));
			oneOf(meth).stringConst("hello"); will(returnValue(e1));
			oneOf(meth).returnObject(e1); will(returnValue(ro1));
			oneOf(meth).stringConst("goodbye"); will(returnValue(e2));
			oneOf(meth).returnObject(e2); will(returnValue(ro2));
			oneOf(meth).callStatic(J.FLEVAL, JavaType.boolean_, "isTruthy", fcx, t1); will(returnValue(ist));
			oneOf(meth).ifBoolean(ist, ro1, ro2);
		}});
		
		gen.startInline(fi);
		gen.visitCase(fcd1);
		gen.visitGuard(fcd1);
		gen.visitExpr(t, 0);
		gen.visitUnresolvedVar(t, 0);
		gen.leaveGuard(fcd1);
		gen.visitExpr(expr, 0);
		gen.visitStringLiteral(expr);
		gen.leaveCase(fcd1);

		gen.visitCase(fcd2);
		gen.visitExpr(expr2, 0);
		gen.visitStringLiteral(expr2);
		gen.leaveCase(fcd2);
		gen.endInline(fi);
	}
	
	/*
	@Test
	public void twoGuardsNoDefault() {
		StackVisitor gen = new StackVisitor();
		gen.push(v);
		gen.push(new GuardGeneratorJS(gen, meth));

		FunctionName name = FunctionName.function(pos, pkg, "x");
		FunctionDefinition fn = new FunctionDefinition(name, 0);
		FunctionIntro fi = new FunctionIntro(name, new ArrayList<>());
		UnresolvedVar t = new UnresolvedVar(pos, "True");
		t.bind(LoadBuiltins.trueT);
		NumericLiteral expr = new NumericLiteral(pos, "42", 2);
		FunctionCaseDefn fcd1 = new FunctionCaseDefn(t, expr);
		fi.functionCase(fcd1);
		UnresolvedVar f = new UnresolvedVar(pos, "False");
		f.bind(LoadBuiltins.falseT);
		FunctionCaseDefn fcd2 = new FunctionCaseDefn(f, expr);
		fi.functionCase(fcd2);
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
		}});
		
		gen.startInline(fi);
		gen.visitCase(fcd1);
		gen.visitGuard(fcd1);
		gen.visitExpr(t, 0);
		gen.visitUnresolvedVar(t, 0);
		gen.leaveGuard(fcd1);
		gen.visitExpr(expr, 0);
		gen.visitNumericLiteral(expr);
		gen.leaveCase(fcd1);
		context.assertIsSatisfied();
		
		JSExpr ge2 = context.mock(JSExpr.class, "ge2");
		JSBlockCreator yesGuard2 = context.mock(JSBlockCreator.class, "yesGuard2");
		JSBlockCreator noGuard2 = context.mock(JSBlockCreator.class, "noGuard2");
		JSIfExpr guard2 = new JSIfExpr(null, yesGuard2, noGuard2);
		JSExpr r2 = context.mock(JSExpr.class, "r2");

		context.checking(new Expectations() {{
			oneOf(noGuard).structConst("False"); will(returnValue(ge2));
			oneOf(noGuard).ifTrue(ge2); will(returnValue(guard2));
			oneOf(yesGuard2).literal("42"); will(returnValue(r2));
			oneOf(yesGuard2).returnObject(r2);
			oneOf(noGuard2).errorNoDefaultGuard();
		}});
		gen.visitCase(fcd2);
		gen.visitGuard(fcd2);
		gen.visitExpr(f, 0);
		gen.visitUnresolvedVar(f, 0);
		gen.leaveGuard(fcd2);
		gen.visitExpr(expr, 0);
		gen.visitNumericLiteral(expr);
		gen.leaveCase(fcd2);
		gen.endInline(fi);
	}
*/
	// two + default
}
