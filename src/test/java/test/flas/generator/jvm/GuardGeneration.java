package test.flas.generator.jvm;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.ApplyExpr;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.compiler.jvmgen.FunctionState;
import org.flasck.flas.compiler.jvmgen.GuardGenerator;
import org.flasck.flas.parsedForm.FunctionCaseDefn;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.parsedForm.UnresolvedOperator;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.repository.LoadBuiltins;
import org.flasck.flas.repository.Repository.Visitor;
import org.flasck.flas.repository.StackVisitor;
import org.flasck.jvm.J;
import org.hamcrest.Matchers;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Rule;
import org.junit.Test;
import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.JavaType;
import org.zinutils.bytecode.MethodDefiner;
import org.zinutils.bytecode.Var.AVar;

public class GuardGeneration {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	private final MethodDefiner meth = context.mock(MethodDefiner.class);
	private final IExpr fcx = context.mock(IExpr.class, "fcx");
	private Visitor v = context.mock(Visitor.class);
	private InputPosition pos = new InputPosition("-", 1, 0, null);
	private final PackageName pkg = new PackageName("test.repo");
	@SuppressWarnings("unchecked")
	private final List<IExpr> block = context.mock(List.class, "block");

	@Test
	public void aSingleGuard() {
		StackVisitor gen = new StackVisitor();
		gen.push(v);
		new GuardGenerator(new FunctionState(meth, fcx, null, null, null, null), gen, block);

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
		IExpr ot1 = context.mock(IExpr.class, "ot1");
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
			oneOf(meth).callStatic("org.flasck.jvm.builtin.FLError", J.OBJECT, "eval", fcx, a2); will(returnValue(err));
			oneOf(meth).returnObject(err); will(returnValue(rerr));
			oneOf(meth).as(t1, J.OBJECT); will(returnValue(ot1));
			oneOf(meth).callInterface(JavaType.boolean_.toString(), fcx, "isTruthy", ot1); will(returnValue(ist));
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
		new GuardGenerator(new FunctionState(meth, fcx, null, null, null, null), gen, block);

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
		IExpr ot1 = context.mock(IExpr.class, "ot1");
		IExpr ro1 = context.mock(IExpr.class, "ro1");
		IExpr e1 = context.mock(IExpr.class, "e1");
		IExpr ist = context.mock(IExpr.class, "ist");
		IExpr e2 = context.mock(IExpr.class, "e2");
		IExpr ro2 = context.mock(IExpr.class, "ro2");

		context.checking(new Expectations() {{
			oneOf(meth).arrayOf(J.OBJECT, new ArrayList<>()); will(returnValue(a1));
			oneOf(meth).callStatic("org.flasck.jvm.builtin.True", J.OBJECT, "eval", fcx, a1); will(returnValue(t1));
			oneOf(meth).stringConst("hello"); will(returnValue(e1));
			oneOf(meth).returnObject(e1); will(returnValue(ro1));
			oneOf(meth).stringConst("goodbye"); will(returnValue(e2));
			oneOf(meth).returnObject(e2); will(returnValue(ro2));
			oneOf(meth).as(t1, J.OBJECT); will(returnValue(ot1));
			oneOf(meth).callInterface(JavaType.boolean_.toString(), fcx, "isTruthy", ot1); will(returnValue(ist));
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

	@SuppressWarnings("unchecked")
	@Test
	public void aSingleGuardWithAComplexExpression() {
		StackVisitor gen = new StackVisitor();
		gen.push(v);
		new GuardGenerator(new FunctionState(meth, fcx, null, null, null, null), gen, block);

		FunctionName name = FunctionName.function(pos, pkg, "x");
		FunctionDefinition fn = new FunctionDefinition(name, 0);
		FunctionIntro fi = new FunctionIntro(name, new ArrayList<>());
		UnresolvedVar t = new UnresolvedVar(pos, "True");
		t.bind(LoadBuiltins.trueT);
		UnresolvedVar length = new UnresolvedVar(pos, "length");
		length.bind(LoadBuiltins.length);
		StringLiteral str = new StringLiteral(pos, "hello");
		ApplyExpr expr = new ApplyExpr(pos, length, str);
		FunctionCaseDefn fcd = new FunctionCaseDefn(t, expr);
		fi.functionCase(fcd);
		fn.intro(fi);

		context.checking(new Expectations() {{
			oneOf(meth).nextLocal(); will(returnValue(6));
		}});
		AVar v1 = new AVar(meth, J.FLCLOSURE, "v1");
		IExpr a1 = context.mock(IExpr.class, "a1");
		IExpr t1 = context.mock(IExpr.class, "t1");
		IExpr len = context.mock(IExpr.class, "len");
		IExpr aslen = context.mock(IExpr.class, "aslen");
		IExpr lenclos = context.mock(IExpr.class, "lenclos");
		IExpr assignV1 = context.mock(IExpr.class, "assignV1");
		IExpr ot1 = context.mock(IExpr.class, "ot1");
		IExpr ro1 = context.mock(IExpr.class, "ro1");
		IExpr blk = context.mock(IExpr.class, "blk");
		IExpr e1 = context.mock(IExpr.class, "e1");
		IExpr ist = context.mock(IExpr.class, "ist");
		IExpr s = context.mock(IExpr.class, "s");
		IExpr a2 = context.mock(IExpr.class, "a2");
		IExpr a3 = context.mock(IExpr.class, "a3");
		IExpr err = context.mock(IExpr.class, "err");
		IExpr rerr = context.mock(IExpr.class, "rerr");

		context.checking(new Expectations() {{
			oneOf(meth).arrayOf(J.OBJECT, new ArrayList<>()); will(returnValue(a1));
			oneOf(meth).callStatic("org.flasck.jvm.builtin.True", J.OBJECT, "eval", fcx, a1); will(returnValue(t1));
			oneOf(meth).classConst(J.BUILTINPKG+".PACKAGEFUNCTIONS$length"); will(returnValue(len));
			oneOf(meth).makeNew(J.CALLEVAL, len); will(returnValue(len));
			oneOf(meth).stringConst("hello"); will(returnValue(e1));
			oneOf(meth).arrayOf(with(J.OBJECT), (List<IExpr>) with(Matchers.contains(e1))); will(returnValue(a3));
			oneOf(meth).as(len, J.APPLICABLE); will(returnValue(aslen));
			oneOf(meth).callInterface(J.FLCLOSURE, fcx, "closure", aslen, a3); will(returnValue(lenclos));
			oneOf(meth).avar(J.FLCLOSURE, "v1"); will(returnValue(v1));
			oneOf(meth).assign(v1, lenclos); will(returnValue(assignV1));
			oneOf(meth).returnObject(v1); will(returnValue(ro1));
			oneOf(meth).block(assignV1, ro1); will(returnValue(blk));
			oneOf(meth).stringConst("no default guard"); will(returnValue(s));
			oneOf(meth).arrayOf(J.OBJECT, s); will(returnValue(a2));
			oneOf(meth).callStatic("org.flasck.jvm.builtin.FLError", J.OBJECT, "eval", fcx, a2); will(returnValue(err));
			oneOf(meth).returnObject(err); will(returnValue(rerr));
			oneOf(meth).as(t1, J.OBJECT); will(returnValue(ot1));
			oneOf(meth).callInterface(JavaType.boolean_.toString(), fcx, "isTruthy", ot1); will(returnValue(ist));
			oneOf(meth).ifBoolean(ist, blk, rerr);
		}});
		
		gen.startInline(fi);
		gen.visitCase(fcd);
		gen.visitGuard(fcd);
		gen.visitExpr(t, 0);
		gen.visitUnresolvedVar(t, 0);
		gen.leaveGuard(fcd);
		gen.visitExpr(expr, 1);
		gen.visitApplyExpr(expr);
		gen.visitExpr(length, 1);
		gen.visitUnresolvedVar(length, 1);
		gen.visitExpr(str, 0);
		gen.visitStringLiteral(str);
		gen.leaveApplyExpr(expr);
		gen.leaveCase(fcd);
		gen.endInline(fi);
	}

	@Test
	public void twoGuardsNoDefault() {
		StackVisitor gen = new StackVisitor();
		gen.push(v);
		new GuardGenerator(new FunctionState(meth, fcx, null, null, null, null), gen, block);

		FunctionName name = FunctionName.function(pos, pkg, "x");
		FunctionDefinition fn = new FunctionDefinition(name, 0);
		FunctionIntro fi = new FunctionIntro(name, new ArrayList<>());
		UnresolvedVar t = new UnresolvedVar(pos, "True");
		t.bind(LoadBuiltins.trueT);
		StringLiteral expr = new StringLiteral(pos, "hello");
		FunctionCaseDefn fcd1 = new FunctionCaseDefn(t, expr);
		fi.functionCase(fcd1);
		UnresolvedVar f = new UnresolvedVar(pos, "False");
		f.bind(LoadBuiltins.falseT);
		StringLiteral expr2 = new StringLiteral(pos, "goodbye");
		FunctionCaseDefn fcd2 = new FunctionCaseDefn(f, expr2);
		fi.functionCase(fcd2);
		fn.intro(fi);

		IExpr a1 = context.mock(IExpr.class, "a1");
		IExpr t1 = context.mock(IExpr.class, "t1");
		IExpr ot1 = context.mock(IExpr.class, "ot1");
		IExpr ro1 = context.mock(IExpr.class, "ro1");
		IExpr e1 = context.mock(IExpr.class, "e1");

		context.checking(new Expectations() {{
			oneOf(meth).arrayOf(J.OBJECT, new ArrayList<>()); will(returnValue(a1));
			oneOf(meth).callStatic("org.flasck.jvm.builtin.True", J.OBJECT, "eval", fcx, a1); will(returnValue(t1));
			oneOf(meth).stringConst("hello"); will(returnValue(e1));
			oneOf(meth).returnObject(e1); will(returnValue(ro1));
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
		context.assertIsSatisfied();
		
		IExpr a2 = context.mock(IExpr.class, "a2");
		IExpr f1 = context.mock(IExpr.class, "f1");
		IExpr of1 = context.mock(IExpr.class, "of1");
		IExpr e2 = context.mock(IExpr.class, "e2");
		IExpr ro2 = context.mock(IExpr.class, "ro2");
		IExpr ndg = context.mock(IExpr.class, "ndg");
		IExpr a3 = context.mock(IExpr.class, "a3");
		IExpr err = context.mock(IExpr.class, "err");
		IExpr rerr = context.mock(IExpr.class, "rerr");
		IExpr isf = context.mock(IExpr.class, "isf");
		IExpr ist = context.mock(IExpr.class, "ist");
		IExpr if1 = context.mock(IExpr.class, "if1");
		IExpr if2 = context.mock(IExpr.class, "if2");

		context.checking(new Expectations() {{
			oneOf(meth).arrayOf(J.OBJECT, new ArrayList<>()); will(returnValue(a2));
			oneOf(meth).callStatic("org.flasck.jvm.builtin.False", J.OBJECT, "eval", fcx, a2); will(returnValue(f1));
			oneOf(meth).stringConst("goodbye"); will(returnValue(e2));
			oneOf(meth).returnObject(e2); will(returnValue(ro2));
			oneOf(meth).stringConst("no default guard"); will(returnValue(ndg));
			oneOf(meth).arrayOf(J.OBJECT, ndg); will(returnValue(a3));
			oneOf(meth).callStatic("org.flasck.jvm.builtin.FLError", J.OBJECT, "eval", fcx, a3); will(returnValue(err));
			oneOf(meth).returnObject(err); will(returnValue(rerr));
			oneOf(meth).as(f1, J.OBJECT); will(returnValue(of1));
			oneOf(meth).callInterface(JavaType.boolean_.toString(), fcx, "isTruthy", of1); will(returnValue(isf));
			oneOf(meth).ifBoolean(isf, ro2, rerr); will(returnValue(if1));
			oneOf(meth).as(t1, J.OBJECT); will(returnValue(ot1));
			oneOf(meth).callInterface(JavaType.boolean_.toString(), fcx, "isTruthy", ot1); will(returnValue(ist));
			oneOf(meth).ifBoolean(ist, ro1, if1); will(returnValue(if2));
		}});
		gen.visitCase(fcd2);
		gen.visitGuard(fcd2);
		gen.visitExpr(f, 0);
		gen.visitUnresolvedVar(f, 0);
		gen.leaveGuard(fcd2);
		gen.visitExpr(expr2, 0);
		gen.visitStringLiteral(expr2);
		gen.leaveCase(fcd2);
		gen.endInline(fi);
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void twoGuardsWithSecondBeingAnExpression() {
		StackVisitor gen = new StackVisitor();
		gen.push(v);
		new GuardGenerator(new FunctionState(meth, fcx, null, null, null, null), gen, block);

		FunctionName name = FunctionName.function(pos, pkg, "x");
		FunctionDefinition fn = new FunctionDefinition(name, 0);
		FunctionIntro fi = new FunctionIntro(name, new ArrayList<>());
		
		UnresolvedVar t = new UnresolvedVar(pos, "True");
		t.bind(LoadBuiltins.trueT);
		StringLiteral expr1 = new StringLiteral(pos, "hello");
		FunctionCaseDefn fcd1 = new FunctionCaseDefn(t, expr1);
		fi.functionCase(fcd1);

		UnresolvedOperator isEq = new UnresolvedOperator(pos, "==");
		isEq.bind(LoadBuiltins.isEqual);
		StringLiteral str = new StringLiteral(pos, "goodbye");
		ApplyExpr g2 = new ApplyExpr(pos, isEq, expr1, str);
		UnresolvedVar length = new UnresolvedVar(pos, "length");
		length.bind(LoadBuiltins.length);
		ApplyExpr expr2 = new ApplyExpr(pos, length, str);
		FunctionCaseDefn fcd2 = new FunctionCaseDefn(g2, expr1);
		fi.functionCase(fcd2);
		
		fn.intro(fi);

		context.checking(new Expectations() {{
			oneOf(meth).nextLocal(); will(returnValue(6));
			oneOf(meth).nextLocal(); will(returnValue(12));
		}});
		AVar v1 = new AVar(meth, J.FLCLOSURE, "v1");
		AVar v2 = new AVar(meth, J.FLCLOSURE, "v2");
		IExpr a1 = context.mock(IExpr.class, "a1");
		IExpr t1 = context.mock(IExpr.class, "t1");
		IExpr len = context.mock(IExpr.class, "len");
		IExpr aslen = context.mock(IExpr.class, "aslen");
		IExpr lenclos = context.mock(IExpr.class, "lenclos");
		IExpr assignV1 = context.mock(IExpr.class, "assignV1");
		IExpr assignV2 = context.mock(IExpr.class, "assignV2");
		IExpr ot1 = context.mock(IExpr.class, "ot1");
		IExpr ov1 = context.mock(IExpr.class, "of1");
		IExpr ro1 = context.mock(IExpr.class, "ro1");
		IExpr ro2 = context.mock(IExpr.class, "ro2");
		IExpr blk1 = context.mock(IExpr.class, "blk1");
		IExpr blk2 = context.mock(IExpr.class, "blk2");
		IExpr e1 = context.mock(IExpr.class, "e1");
		IExpr e2 = context.mock(IExpr.class, "e2");
		IExpr ist = context.mock(IExpr.class, "ist");
		IExpr isg = context.mock(IExpr.class, "isf");
		IExpr s = context.mock(IExpr.class, "s");
		IExpr a2 = context.mock(IExpr.class, "a2");
		IExpr a3 = context.mock(IExpr.class, "a3");
		IExpr a4 = context.mock(IExpr.class, "a4");
		IExpr err = context.mock(IExpr.class, "err");
		IExpr rerr = context.mock(IExpr.class, "rerr");
		IExpr if1 = context.mock(IExpr.class, "if1");
		IExpr ise = context.mock(IExpr.class, "ise");
		IExpr asise = context.mock(IExpr.class, "asise");
		IExpr iseclos = context.mock(IExpr.class, "iseclos");

		context.checking(new Expectations() {{
			oneOf(meth).arrayOf(J.OBJECT, new ArrayList<>()); will(returnValue(a1));
			oneOf(meth).callStatic("org.flasck.jvm.builtin.True", J.OBJECT, "eval", fcx, a1); will(returnValue(t1));
			
			oneOf(meth).stringConst("hello"); will(returnValue(e1));
			oneOf(meth).returnObject(e1); will(returnValue(ro1));
			
			oneOf(meth).classConst(J.FLEVAL+"$IsEqual"); will(returnValue(ise));
			oneOf(meth).makeNew(J.CALLEVAL, ise); will(returnValue(ise));
			oneOf(meth).stringConst("goodbye"); will(returnValue(e2));
			oneOf(meth).stringConst("goodbye"); will(returnValue(e2));
			oneOf(meth).arrayOf(with(J.OBJECT), (List<IExpr>) with(Matchers.contains(e2, e2))); will(returnValue(a4));
			oneOf(meth).as(ise, J.APPLICABLE); will(returnValue(asise));
			oneOf(meth).callInterface(J.FLCLOSURE, fcx, "closure", asise, a4); will(returnValue(iseclos));
			oneOf(meth).avar(J.FLCLOSURE, "v1"); will(returnValue(v1));
			oneOf(meth).assign(v1, iseclos); will(returnValue(assignV1));
			
			oneOf(meth).classConst(J.BUILTINPKG+".PACKAGEFUNCTIONS$length"); will(returnValue(len));
			oneOf(meth).makeNew(J.CALLEVAL, len); will(returnValue(len));
			oneOf(meth).stringConst("goodbye"); will(returnValue(e2));
			oneOf(meth).arrayOf(with(J.OBJECT), (List<IExpr>) with(Matchers.contains(e2))); will(returnValue(a3));
			oneOf(meth).as(len, J.APPLICABLE); will(returnValue(aslen));
			oneOf(meth).callInterface(J.FLCLOSURE, fcx, "closure", aslen, a3); will(returnValue(lenclos));
			oneOf(meth).avar(J.FLCLOSURE, "v2"); will(returnValue(v2));
			oneOf(meth).assign(v2, lenclos); will(returnValue(assignV2));
			oneOf(meth).returnObject(v2); will(returnValue(ro2));
			oneOf(meth).block(assignV2, ro2); will(returnValue(blk1));
			
			oneOf(meth).stringConst("no default guard"); will(returnValue(s));
			oneOf(meth).arrayOf(J.OBJECT, s); will(returnValue(a2));
			oneOf(meth).callStatic("org.flasck.jvm.builtin.FLError", J.OBJECT, "eval", fcx, a2); will(returnValue(err));
			oneOf(meth).returnObject(err); will(returnValue(rerr));
			
			oneOf(meth).as(v1, J.OBJECT); will(returnValue(ov1));
			oneOf(meth).callInterface(JavaType.boolean_.toString(), fcx, "isTruthy", ov1); will(returnValue(isg));
			oneOf(meth).ifBoolean(isg, blk1, rerr); will(returnValue(if1));
			oneOf(meth).block(assignV1, if1); will(returnValue(blk2));
			
			oneOf(meth).as(t1, J.OBJECT); will(returnValue(ot1));
			oneOf(meth).callInterface(JavaType.boolean_.toString(), fcx, "isTruthy", ot1); will(returnValue(ist));
			oneOf(meth).ifBoolean(ist, ro1, blk2);
		}});
		
		gen.startInline(fi);
		
		gen.visitCase(fcd1);
		gen.visitGuard(fcd1);
		gen.visitExpr(t, 0);
		gen.visitUnresolvedVar(t, 0);
		gen.leaveGuard(fcd1);
		gen.visitExpr(expr1, 0);
		gen.visitStringLiteral(expr1);
		gen.leaveCase(fcd1);

		gen.visitCase(fcd2);
		gen.visitGuard(fcd2);
		gen.visitExpr(g2, 0);
		gen.visitApplyExpr(g2);
		gen.visitExpr(isEq, 2);
		gen.visitUnresolvedOperator(isEq, 2);
		gen.visitExpr(str, 0);
		gen.visitStringLiteral(str);
		gen.visitExpr(str, 0);
		gen.visitStringLiteral(str);
		gen.leaveApplyExpr(g2);
		gen.leaveGuard(fcd2);
		gen.visitExpr(expr2, 1);
		gen.visitApplyExpr(expr2);
		gen.visitExpr(length, 1);
		gen.visitUnresolvedVar(length, 1);
		gen.visitExpr(str, 0);
		gen.visitStringLiteral(str);
		gen.leaveApplyExpr(expr2);
		gen.leaveCase(fcd2);

		gen.endInline(fi);
	}

	// two + default
}
