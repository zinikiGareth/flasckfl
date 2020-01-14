package test.flas.generator.jvm;

import java.util.ArrayList;
import java.util.Arrays;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.NumericLiteral;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.compiler.jvmgen.JVMGenerator;
import org.flasck.flas.hsi.ArgSlot;
import org.flasck.flas.hsi.CMSlot;
import org.flasck.flas.hsi.Slot;
import org.flasck.flas.parsedForm.FunctionCaseDefn;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.patterns.HSIArgsTree;
import org.flasck.flas.patterns.HSIPatternOptions;
import org.flasck.flas.repository.LoadBuiltins;
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
import org.zinutils.bytecode.JavaType;
import org.zinutils.bytecode.MethodDefiner;
import org.zinutils.bytecode.Var;
import org.zinutils.bytecode.JavaInfo.Access;
import org.zinutils.bytecode.mock.VarMatcher;
import org.zinutils.support.jmock.CaptureAction;

public class FunctionGeneration {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	private InputPosition pos = new InputPosition("-", 1, 0, null);
	private final PackageName pkg = new PackageName("test.repo");
	private final ByteCodeStorage bce = context.mock(ByteCodeStorage.class);
	private final ByteCodeSink bcc = context.mock(ByteCodeSink.class);
	private final MethodDefiner meth = context.mock(MethodDefiner.class);

	@Before
	public void setup() {
		context.checking(new Expectations() {{
			allowing(bcc).generateAssociatedSourceFile();
			allowing(meth).lenientMode(with(any(Boolean.class)));
		}});
	}

	@Test
	public void varsAreNamedAfterSlotID() {
		context.checking(new Expectations() {{
			oneOf(meth).nextLocal(); will(returnValue(22));
			oneOf(meth).nextLocal(); will(returnValue(23));
		}});
		Var args = new Var.AVar(meth, "JVMRunner", "runner");
		Var cxt = new Var.AVar(meth, J.FLEVALCONTEXT, "cxt");
		StackVisitor sv = new StackVisitor();
		new JVMGenerator(bce, sv);
		Slot slot = new ArgSlot(3, null);
		IExpr arg0 = context.mock(IExpr.class, "arg0");
		IExpr head = context.mock(IExpr.class, "head");
		IExpr ass1 = context.mock(IExpr.class, "ass1");
		context.checking(new Expectations() {{
			oneOf(bce).newClass("test.repo.PACKAGEFUNCTIONS$x"); will(returnValue(bcc));
			oneOf(bcc).defineField(true, Access.PUBLICSTATIC, JavaType.int_, "nfargs");
			oneOf(bcc).createMethod(true, "java.lang.Object", "eval"); will(returnValue(meth));
			oneOf(meth).argument(J.FLEVALCONTEXT, "cxt"); will(returnValue(cxt));
			oneOf(meth).argument("[java.lang.Object", "args"); will(returnValue(args));
			oneOf(meth).nextLocal(); will(returnValue(25));
			oneOf(meth).arrayItem(J.OBJECT, args, 3); will(returnValue(arg0));
			oneOf(meth).callInterface(J.OBJECT, cxt, "head", arg0); will(returnValue(head));
			oneOf(meth).assign(with(VarMatcher.local(25)), with(head)); will(returnValue(ass1));
		}});
		FunctionName name = FunctionName.function(pos, pkg, "x");
		FunctionIntro fi = new FunctionIntro(name, new ArrayList<>());
		FunctionDefinition fn = new FunctionDefinition(name, 1);
		fn.intro(fi);
		sv.visitFunction(fn);
		sv.bind(slot, "x");
	}
	
	@Test
	public void aSimpleFunction() {
		IExpr iret = context.mock(IExpr.class, "ret");
		IExpr nret = context.mock(IExpr.class, "nret");
		IExpr nullVal = context.mock(IExpr.class, "null");
		IExpr biv = context.mock(IExpr.class, "biv");
		IExpr cdv = context.mock(IExpr.class, "cdv");
		IExpr re = context.mock(IExpr.class, "re");
		context.checking(new Expectations() {{
			oneOf(meth).nextLocal(); will(returnValue(22));
			oneOf(meth).nextLocal(); will(returnValue(23));
		}});
		Var cxt = new Var.AVar(meth, J.FLEVALCONTEXT, "cxt");
		Var args = new Var.AVar(meth, "JVMRunner", "runner");
		context.checking(new Expectations() {{
			oneOf(bce).newClass("test.repo.PACKAGEFUNCTIONS$x"); will(returnValue(bcc));
			oneOf(bcc).defineField(true, Access.PUBLICSTATIC, JavaType.int_, "nfargs");
			oneOf(bcc).createMethod(true, "java.lang.Object", "eval"); will(returnValue(meth));
			oneOf(meth).argument(J.FLEVALCONTEXT, "cxt"); will(returnValue(cxt));
			oneOf(meth).argument("[java.lang.Object", "args"); will(returnValue(args));

			oneOf(meth).intConst(42); will(returnValue(iret));
			oneOf(meth).aNull(); will(returnValue(nullVal));
			oneOf(meth).box(iret); will(returnValue(biv));
			oneOf(meth).castTo(nullVal, "java.lang.Double"); will(returnValue(cdv));
			oneOf(meth).makeNew("org.flasck.jvm.builtin.FLNumber", biv, cdv); will(returnValue(nret));
			oneOf(meth).returnObject(nret); will(returnValue(re));
			oneOf(re).flush();
		}});
		FunctionName name = FunctionName.function(pos, pkg, "x");
		FunctionDefinition fn = new FunctionDefinition(name, 0);
		FunctionIntro fi = new FunctionIntro(name, new ArrayList<>());
		FunctionCaseDefn fcd = new FunctionCaseDefn(null, new NumericLiteral(pos, 42));
		fi.functionCase(fcd);
		fn.intro(fi);
		HSIArgsTree hsi = new HSIArgsTree(0);
		hsi.consider(fi);
		fn.bindHsi(hsi);
		StackVisitor sv = new StackVisitor();
		new JVMGenerator(bce, sv);
		new Traverser(sv).withHSI().visitFunction(fn);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void aMinimalHSIFunction() {
		IExpr head = context.mock(IExpr.class, "head");
		IExpr nsc = context.mock(IExpr.class, "nsc");
		IExpr ansc = context.mock(IExpr.class, "ansc");
		IExpr eNSC = context.mock(IExpr.class, "ensc");
		IExpr rerr = context.mock(IExpr.class, "rerr");
		IExpr doif = context.mock(IExpr.class, "doif");
		IExpr nil = context.mock(IExpr.class, "nil");
		IExpr isA = context.mock(IExpr.class, "isA");
		IExpr sret = context.mock(IExpr.class, "sret");
		IExpr re = context.mock(IExpr.class, "re");
		context.checking(new Expectations() {{
			oneOf(meth).nextLocal(); will(returnValue(22));
			oneOf(meth).nextLocal(); will(returnValue(23));
		}});
		Var cxt = new Var.AVar(meth, J.FLEVALCONTEXT, "cxt");
		Var args = new Var.AVar(meth, "[Object", "args");
		IExpr arg0 = context.mock(IExpr.class, "arg0");
		IExpr ass1 = context.mock(IExpr.class, "ass1");
		IExpr blk1 = context.mock(IExpr.class, "blk1");
		context.checking(new Expectations() {{
			oneOf(bce).newClass("test.repo.PACKAGEFUNCTIONS$x"); will(returnValue(bcc));
			oneOf(bcc).createMethod(true, "java.lang.Object", "eval"); will(returnValue(meth));
			oneOf(bcc).defineField(true, Access.PUBLICSTATIC, JavaType.int_, "nfargs");
			oneOf(meth).argument(J.FLEVALCONTEXT, "cxt"); will(returnValue(cxt));
			oneOf(meth).argument("[java.lang.Object", "args"); will(returnValue(args));

			oneOf(meth).nextLocal(); will(returnValue(25));
			oneOf(meth).arrayItem(J.OBJECT, args, 0); will(returnValue(arg0));
			oneOf(meth).callInterface(J.OBJECT, cxt, "head", arg0); will(returnValue(head));
			oneOf(meth).assign(with(VarMatcher.local(25)), with(head)); will(returnValue(ass1));
			oneOf(meth).stringConst("hello"); will(returnValue(sret));
			oneOf(meth).returnObject(sret); will(returnValue(re));
			
			oneOf(meth).stringConst("no matching case"); will(returnValue(nsc));
			oneOf(meth).arrayOf(J.OBJECT, nsc); will (returnValue(ansc));
			oneOf(meth).callStatic(J.FLERROR, J.OBJECT, "eval", cxt, ansc); will(returnValue(eNSC));
			oneOf(meth).returnObject(eNSC); will(returnValue(rerr));
			
			oneOf(meth).stringConst("Nil"); will(returnValue(nil));
			oneOf(meth).callInterface(with(JavaType.boolean_.toString()), with(cxt), with("isA"), with(Matchers.array(VarMatcher.local(25), Matchers.is(nil)))); will(returnValue(isA));
			
			oneOf(meth).ifBoolean(isA, re, rerr); will(returnValue(doif));
			oneOf(meth).block(ass1, doif); will(returnValue(blk1));
			oneOf(blk1).flush();
		}});
		FunctionName name = FunctionName.function(pos, pkg, "x");
		FunctionDefinition fn = new FunctionDefinition(name, 1);
		FunctionIntro fi = new FunctionIntro(name, new ArrayList<>());
		FunctionCaseDefn fcd = new FunctionCaseDefn(null, new StringLiteral(pos, "hello"));
		fi.functionCase(fcd);
		fn.intro(fi);
		HSIArgsTree hsi = new HSIArgsTree(1);
		hsi.consider(fi);
		hsi.get(0).requireCM(LoadBuiltins.nil).consider(fi);
		fn.bindHsi(hsi);
		StackVisitor sv = new StackVisitor();
		new JVMGenerator(bce, sv);
		new Traverser(sv).withHSI().visitFunction(fn);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void aTwoConstructorHSIFunction() {
		IExpr head = context.mock(IExpr.class, "head");
		IExpr nsc = context.mock(IExpr.class, "nsc");
		IExpr ansc = context.mock(IExpr.class, "ansc");
		IExpr eNSC = context.mock(IExpr.class, "ensc");
		IExpr rerr = context.mock(IExpr.class, "rerr");
		IExpr doif = context.mock(IExpr.class, "doif");
		IExpr doif2 = context.mock(IExpr.class, "doif2");
		IExpr nil = context.mock(IExpr.class, "nil");
		IExpr cons = context.mock(IExpr.class, "cons");
		IExpr isA = context.mock(IExpr.class, "isA");
		IExpr isACons = context.mock(IExpr.class, "isACons");
		IExpr gret = context.mock(IExpr.class, "gret");
		IExpr gr = context.mock(IExpr.class, "gr");
		IExpr sret = context.mock(IExpr.class, "sret");
		IExpr re = context.mock(IExpr.class, "re");
		context.checking(new Expectations() {{
			oneOf(meth).nextLocal(); will(returnValue(22));
			oneOf(meth).nextLocal(); will(returnValue(23));
		}});
		Var cxt = new Var.AVar(meth, J.FLEVALCONTEXT, "cxt");
		Var args = new Var.AVar(meth, "[Object", "args");
		IExpr arg0 = context.mock(IExpr.class, "arg0");
		IExpr ass1 = context.mock(IExpr.class, "ass1");
		IExpr blk1 = context.mock(IExpr.class, "blk1");
		context.checking(new Expectations() {{
			oneOf(bce).newClass("test.repo.PACKAGEFUNCTIONS$x"); will(returnValue(bcc));
			oneOf(bcc).defineField(true, Access.PUBLICSTATIC, JavaType.int_, "nfargs");
			oneOf(bcc).createMethod(true, "java.lang.Object", "eval"); will(returnValue(meth));
			oneOf(meth).argument(J.FLEVALCONTEXT, "cxt"); will(returnValue(cxt));
			oneOf(meth).argument("[java.lang.Object", "args"); will(returnValue(args));

			oneOf(meth).nextLocal(); will(returnValue(25));
			oneOf(meth).arrayItem(J.OBJECT, args, 0); will(returnValue(arg0));
			oneOf(meth).callInterface(J.OBJECT, cxt, "head", arg0); will(returnValue(head));
			oneOf(meth).assign(with(VarMatcher.local(25)), with(head)); will(returnValue(ass1));
			
			oneOf(meth).stringConst("goodbye"); will(returnValue(gret));
			oneOf(meth).returnObject(gret); will(returnValue(gr));
			
			oneOf(meth).stringConst("hello"); will(returnValue(sret));
			oneOf(meth).returnObject(sret); will(returnValue(re));
			
			oneOf(meth).stringConst("no matching case"); will(returnValue(nsc));
			oneOf(meth).arrayOf(J.OBJECT, nsc); will (returnValue(ansc));
			oneOf(meth).callStatic(J.FLERROR, J.OBJECT, "eval", cxt, ansc); will(returnValue(eNSC));
			oneOf(meth).returnObject(eNSC); will(returnValue(rerr));
			
			oneOf(meth).stringConst("Nil"); will(returnValue(nil));
			oneOf(meth).callInterface(with(JavaType.boolean_.toString()), with(cxt), with("isA"), with(Matchers.array(VarMatcher.local(25), Matchers.is(nil)))); will(returnValue(isA));
			
			oneOf(meth).stringConst("Cons"); will(returnValue(cons));
			oneOf(meth).callInterface(with(JavaType.boolean_.toString()), with(cxt), with("isA"), with(Matchers.array(VarMatcher.local(25), Matchers.is(cons)))); will(returnValue(isACons));
			
			oneOf(meth).ifBoolean(isA, re, rerr); will(returnValue(doif));
			oneOf(meth).ifBoolean(isACons, gr, doif); will(returnValue(doif2));
			oneOf(meth).block(ass1, doif2); will(returnValue(blk1));
			oneOf(blk1).flush();
		}});
		FunctionName name = FunctionName.function(pos, pkg, "x");
		FunctionDefinition fn = new FunctionDefinition(name, 1);
		FunctionIntro f1 = new FunctionIntro(name, new ArrayList<>());
		{
			FunctionCaseDefn fcd = new FunctionCaseDefn(null, new StringLiteral(pos, "hello"));
			f1.functionCase(fcd);
			fn.intro(f1);
		}
		FunctionIntro f2 = new FunctionIntro(name, new ArrayList<>());
		{
			FunctionCaseDefn fcd = new FunctionCaseDefn(null, new StringLiteral(pos, "goodbye"));
			f2.functionCase(fcd);
			fn.intro(f2);
		}
		HSIArgsTree hsi = new HSIArgsTree(1);
		hsi.consider(f1);
		hsi.get(0).requireCM(LoadBuiltins.nil).consider(f1);
		hsi.consider(f2);
		hsi.get(0).requireCM(LoadBuiltins.cons).consider(f2);
		fn.bindHsi(hsi);
		StackVisitor sv = new StackVisitor();
		new JVMGenerator(bce, sv);
		new Traverser(sv).withHSI().visitFunction(fn);
	}
	
	@Test
	public void handleAField() {
		IExpr cxt = context.mock(IExpr.class, "cxt");
		IExpr dummy = context.mock(IExpr.class, "dummy");
		context.checking(new Expectations() {{
			oneOf(meth).nextLocal(); will(returnValue(22));
		}});
		Var args = new Var.AVar(meth, "[Object", "args");

		JVMGenerator gen = JVMGenerator.forTests(meth, cxt, args);
		StackVisitor sv = (StackVisitor) gen.stackVisitor();
		FunctionName name = FunctionName.function(pos, pkg, "x");
		
		FunctionIntro intro = new FunctionIntro(name, new ArrayList<>());
		StringLiteral expr = new StringLiteral(pos, "hello");
		intro.functionCase(new FunctionCaseDefn(null, expr));
		
		IExpr ass1 = context.mock(IExpr.class, "ass1");
		CaptureAction captureHead0 = new CaptureAction(ass1);
		context.checking(new Expectations() {{
			oneOf(meth).arrayItem(J.OBJECT, args, 0); will(returnValue(dummy));
			oneOf(meth).nextLocal(); will(returnValue(25));
			oneOf(meth).callInterface(J.OBJECT, cxt, "head", dummy); will(returnValue(dummy));
			oneOf(meth).assign(with(VarMatcher.local(25)), with(dummy)); will(captureHead0);
		}});
		ArgSlot a0 = new ArgSlot(0, new HSIPatternOptions());
		sv.hsiArgs(Arrays.asList(a0));
		sv.switchOn(a0);
		sv.withConstructor("Cons");
		context.assertIsSatisfied();
		IExpr var25 = (IExpr) captureHead0.get(0);
		
		HSIPatternOptions headOpts = new HSIPatternOptions();
		headOpts.includes(intro);
		Slot cm1 = new CMSlot("0_head", headOpts);
		context.checking(new Expectations() {{
			oneOf(meth).stringConst("head"); will(returnValue(dummy));
			oneOf(meth).callStatic(J.FLEVAL, J.OBJECT, "field", cxt, var25, dummy); will(returnValue(dummy));
		}});
		sv.constructorField(a0, "head", cm1);

		IExpr ass2 = context.mock(IExpr.class, "ass2");
		CaptureAction captureHead1 = new CaptureAction(ass2);
		context.checking(new Expectations() {{
			oneOf(meth).nextLocal(); will(returnValue(27));
			oneOf(meth).callInterface(J.OBJECT, cxt, "head", dummy); will(returnValue(dummy));
			oneOf(meth).assign(with(VarMatcher.local(27)), with(dummy)); will(captureHead1);
		}});
		sv.switchOn(cm1);
		sv.withConstructor("True");
	}


	@Test
	public void constructorOrVar() {
		IExpr cxt = context.mock(IExpr.class, "cxt");
		IExpr dummy = context.mock(IExpr.class, "dummy");
		context.checking(new Expectations() {{
			oneOf(meth).nextLocal(); will(returnValue(22));
		}});
		Var args = new Var.AVar(meth, "[Object", "args");

		JVMGenerator gen = JVMGenerator.forTests(meth, cxt, args);
		StackVisitor sv = (StackVisitor) gen.stackVisitor();
		FunctionName name = FunctionName.function(pos, pkg, "x");
		
		IExpr ass1 = context.mock(IExpr.class, "ass1");
		CaptureAction captureHead0 = new CaptureAction(ass1);
		context.checking(new Expectations() {{
			oneOf(meth).arrayItem(J.OBJECT, args, 0); will(returnValue(dummy));
			oneOf(meth).nextLocal(); will(returnValue(25));
			oneOf(meth).callInterface(J.OBJECT, cxt, "head", dummy); will(returnValue(dummy));
			oneOf(meth).assign(with(VarMatcher.local(25)), with(dummy)); will(captureHead0);
		}});
		ArgSlot a0 = new ArgSlot(0, new HSIPatternOptions());
		sv.hsiArgs(Arrays.asList(a0));
		sv.switchOn(a0);
		sv.withConstructor("Nil");
		context.assertIsSatisfied();
		IExpr head0 = (IExpr) captureHead0.get(0);

		FunctionIntro intro = new FunctionIntro(name, new ArrayList<>());
		StringLiteral expr = new StringLiteral(pos, "hello");
		FunctionCaseDefn fcd1 = new FunctionCaseDefn(null, expr);
		intro.functionCase(fcd1);

		IExpr nilExpr = context.mock(IExpr.class, "nilExpr");
		context.checking(new Expectations() {{
			oneOf(meth).stringConst("hello"); will(returnValue(dummy));
			oneOf(meth).returnObject(dummy); will(returnValue(nilExpr));
		}});
		sv.startInline(intro);
		sv.visitCase(fcd1);
		sv.visitExpr(expr, 0);
		sv.visitStringLiteral(expr);
		sv.leaveCase(fcd1);
		sv.endInline(intro);
		context.assertIsSatisfied();
		
		final FunctionIntro intro2;
		intro2 = new FunctionIntro(name, new ArrayList<>());
		NumericLiteral number = new NumericLiteral(pos, "42", 2);
		FunctionCaseDefn fcd2 = new FunctionCaseDefn(null, number);
		intro2.functionCase(fcd2);

		IExpr elseExpr = context.mock(IExpr.class, "elseExpr");
		context.checking(new Expectations() {{
			oneOf(meth).intConst(42); will(returnValue(dummy));
			oneOf(meth).box(dummy); will(returnValue(dummy));
			oneOf(meth).aNull(); will(returnValue(dummy));
			oneOf(meth).castTo(dummy, "java.lang.Double"); will(returnValue(dummy));
			oneOf(meth).makeNew(J.NUMBER, dummy, dummy); will(returnValue(dummy));
			oneOf(meth).returnObject(dummy); will(returnValue(elseExpr));
		}});
		sv.defaultCase();
		sv.startInline(intro2);
		sv.visitCase(fcd2);
		sv.visitExpr(number, 0);
		sv.visitNumericLiteral(number);
		sv.leaveCase(fcd2);
		sv.endInline(intro2);
		context.assertIsSatisfied();

		IExpr ifExpr = context.mock(IExpr.class, "ifExpr");
		context.checking(new Expectations() {{
			oneOf(meth).stringConst("Nil"); will(returnValue(dummy));
			oneOf(meth).callInterface(JavaType.boolean_.toString(), cxt, "isA",head0, dummy); will(returnValue(dummy));
			oneOf(meth).ifBoolean(dummy, nilExpr, elseExpr); will(returnValue(ifExpr));
		}});
		sv.endSwitch();
		context.assertIsSatisfied();

		IExpr blk1 = context.mock(IExpr.class, "blk1");
		context.checking(new Expectations() {{
			oneOf(meth).block(ass1, ifExpr); will(returnValue(blk1));
			oneOf(blk1).flush();
		}});
		sv.leaveFunction(null);
	}

	@Test
	public void numericConstants() {
		IExpr cxt = context.mock(IExpr.class, "cxt");
		IExpr dummy = context.mock(IExpr.class, "dummy");
		context.checking(new Expectations() {{
			oneOf(meth).nextLocal(); will(returnValue(22));
		}});
		Var args = new Var.AVar(meth, "[Object", "args");

		JVMGenerator gen = JVMGenerator.forTests(meth, cxt, args);
		StackVisitor sv = (StackVisitor) gen.stackVisitor();
		FunctionName name = FunctionName.function(pos, pkg, "x");
		
		IExpr ass1 = context.mock(IExpr.class, "ass1");
		CaptureAction captureHead0 = new CaptureAction(ass1);
		context.checking(new Expectations() {{
			oneOf(meth).arrayItem(J.OBJECT, args, 0); will(returnValue(dummy));
			oneOf(meth).nextLocal(); will(returnValue(25));
			oneOf(meth).callInterface(J.OBJECT, cxt, "head", dummy); will(returnValue(dummy));
			oneOf(meth).assign(with(VarMatcher.local(25)), with(dummy)); will(captureHead0);
		}});
		ArgSlot a0 = new ArgSlot(0, new HSIPatternOptions());
		sv.hsiArgs(Arrays.asList(a0));
		sv.switchOn(a0);
		sv.withConstructor("Number");
		context.assertIsSatisfied();
		sv.matchNumber(42);

		FunctionIntro intro = new FunctionIntro(name, new ArrayList<>());
		StringLiteral expr = new StringLiteral(pos, "hello");
		FunctionCaseDefn fcd = new FunctionCaseDefn(null, expr);
		intro.functionCase(fcd);

		IExpr stmt = context.mock(IExpr.class, "stmt");
		context.checking(new Expectations() {{
			oneOf(meth).stringConst("hello"); will(returnValue(dummy));
			oneOf(meth).returnObject(dummy); will(returnValue(stmt));
		}});
		sv.startInline(intro);
		sv.visitCase(fcd);
		sv.visitExpr(expr, 0);
		sv.visitStringLiteral(expr);
		sv.leaveCase(fcd);
		sv.endInline(intro);
		
		IExpr numberNotConst = context.mock(IExpr.class, "numberNotConst");
		context.checking(new Expectations() {{
			oneOf(meth).stringConst("no matching case"); will(returnValue(dummy));
			oneOf(meth).arrayOf(J.OBJECT, dummy); will(returnValue(dummy));
			oneOf(meth).callStatic(J.FLERROR, J.OBJECT, "eval", cxt, dummy); will(returnValue(dummy));
			oneOf(meth).returnObject(dummy); will(returnValue(numberNotConst));
		}});
		sv.matchDefault();
		sv.errorNoCase();
		context.assertIsSatisfied();

		IExpr notNumber = context.mock(IExpr.class, "notNumber");
		context.checking(new Expectations() {{
			oneOf(meth).stringConst("no matching case"); will(returnValue(dummy));
			oneOf(meth).arrayOf(J.OBJECT, dummy); will(returnValue(dummy));
			oneOf(meth).callStatic(J.FLERROR, J.OBJECT, "eval", cxt, dummy); will(returnValue(dummy));
			oneOf(meth).returnObject(dummy); will(returnValue(notNumber));
		}});
		sv.defaultCase();
		sv.errorNoCase();
		
		Var head0 = (Var) captureHead0.get(0);
		IExpr numberBranch = context.mock(IExpr.class, "numberBranch");
		IExpr i42 = context.mock(IExpr.class, "i42");
		IExpr is42 = context.mock(IExpr.class, "is42");
		context.checking(new Expectations() {{
			oneOf(meth).intConst(42); will(returnValue(i42));
			oneOf(meth).callInterface(JavaType.boolean_.toString(), cxt, "isConst", head0, i42); will(returnValue(is42));
			oneOf(meth).ifBoolean(is42, stmt, numberNotConst); will(returnValue(numberBranch));
			oneOf(meth).stringConst("Number"); will(returnValue(dummy));
			oneOf(meth).callInterface(JavaType.boolean_.toString(), cxt, "isA", head0, dummy); will(returnValue(dummy));
			oneOf(meth).ifBoolean(dummy, numberBranch, notNumber); will(returnValue(dummy));
		}});
		sv.endSwitch();
	}


	@Test
	public void stringConstants() {
		IExpr cxt = context.mock(IExpr.class, "cxt");
		IExpr dummy = context.mock(IExpr.class, "dummy");
		context.checking(new Expectations() {{
			oneOf(meth).nextLocal(); will(returnValue(22));
		}});
		Var args = new Var.AVar(meth, "[Object", "args");

		JVMGenerator gen = JVMGenerator.forTests(meth, cxt, args);
		StackVisitor sv = (StackVisitor) gen.stackVisitor();
		FunctionName name = FunctionName.function(pos, pkg, "x");
		
		IExpr ass1 = context.mock(IExpr.class, "ass1");
		CaptureAction captureHead0 = new CaptureAction(ass1);
		context.checking(new Expectations() {{
			oneOf(meth).arrayItem(J.OBJECT, args, 0); will(returnValue(dummy));
			oneOf(meth).nextLocal(); will(returnValue(25));
			oneOf(meth).callInterface(J.OBJECT, cxt, "head", dummy); will(returnValue(dummy));
			oneOf(meth).assign(with(VarMatcher.local(25)), with(dummy)); will(captureHead0);
		}});
		ArgSlot a0 = new ArgSlot(0, new HSIPatternOptions());
		sv.hsiArgs(Arrays.asList(a0));
		sv.switchOn(a0);
		sv.withConstructor("Number");
		sv.matchString("hello");
		context.assertIsSatisfied();

		FunctionIntro intro = new FunctionIntro(name, new ArrayList<>());
		StringLiteral expr = new StringLiteral(pos, "hello");
		FunctionCaseDefn fcd = new FunctionCaseDefn(null, expr);
		intro.functionCase(fcd);

		IExpr stmt = context.mock(IExpr.class, "stmt");
		context.checking(new Expectations() {{
			oneOf(meth).stringConst("hello"); will(returnValue(dummy));
			oneOf(meth).returnObject(dummy); will(returnValue(stmt));
		}});
		sv.startInline(intro);
		sv.visitCase(fcd);
		sv.visitExpr(expr, 0);
		sv.visitStringLiteral(expr);
		sv.leaveCase(fcd);
		sv.endInline(intro);
		
		IExpr numberNotConst = context.mock(IExpr.class, "numberNotConst");
		context.checking(new Expectations() {{
			oneOf(meth).stringConst("no matching case"); will(returnValue(dummy));
			oneOf(meth).arrayOf(J.OBJECT, dummy); will(returnValue(dummy));
			oneOf(meth).callStatic(J.FLERROR, J.OBJECT, "eval", cxt, dummy); will(returnValue(dummy));
			oneOf(meth).returnObject(dummy); will(returnValue(numberNotConst));
		}});
		sv.matchDefault();
		sv.errorNoCase();
		context.assertIsSatisfied();

		IExpr notNumber = context.mock(IExpr.class, "notNumber");
		context.checking(new Expectations() {{
			oneOf(meth).stringConst("no matching case"); will(returnValue(dummy));
			oneOf(meth).arrayOf(J.OBJECT, dummy); will(returnValue(dummy));
			oneOf(meth).callStatic(J.FLERROR, J.OBJECT, "eval", cxt, dummy); will(returnValue(dummy));
			oneOf(meth).returnObject(dummy); will(returnValue(notNumber));
		}});
		sv.defaultCase();
		sv.errorNoCase();
		
		Var head0 = (Var) captureHead0.get(0);
		IExpr numberBranch = context.mock(IExpr.class, "numberBranch");
		IExpr shello = context.mock(IExpr.class, "i42");
		IExpr is42 = context.mock(IExpr.class, "is42");
		context.checking(new Expectations() {{
			oneOf(meth).stringConst("hello"); will(returnValue(shello));
			oneOf(meth).callInterface(JavaType.boolean_.toString(), cxt, "isConst", head0, shello); will(returnValue(is42));
			oneOf(meth).ifBoolean(is42, stmt, numberNotConst); will(returnValue(numberBranch));
			oneOf(meth).stringConst("Number"); will(returnValue(dummy));
			oneOf(meth).callInterface(JavaType.boolean_.toString(), cxt, "isA", head0, dummy); will(returnValue(dummy));
			oneOf(meth).ifBoolean(dummy, numberBranch, notNumber); will(returnValue(dummy));
		}});
		sv.endSwitch();
	}

	@Test
	public void nestedSwitching() {
		IExpr cxt = context.mock(IExpr.class, "cxt");
		IExpr dummy = context.mock(IExpr.class, "dummy");
		context.checking(new Expectations() {{
			oneOf(meth).nextLocal(); will(returnValue(22));
		}});
		Var args = new Var.AVar(meth, "[Object", "args");

		JVMGenerator gen = JVMGenerator.forTests(meth, cxt, args);
		StackVisitor sv = (StackVisitor) gen.stackVisitor();
		FunctionName name = FunctionName.function(pos, pkg, "x");
		
		IExpr ass1 = context.mock(IExpr.class, "ass1");
		CaptureAction captureHead0 = new CaptureAction(ass1);
		context.checking(new Expectations() {{
			oneOf(meth).arrayItem(J.OBJECT, args, 0); will(returnValue(dummy));
			oneOf(meth).nextLocal(); will(returnValue(25));
			oneOf(meth).callInterface(J.OBJECT, cxt, "head", dummy); will(returnValue(dummy));
			oneOf(meth).assign(with(VarMatcher.local(25)), with(dummy)); will(captureHead0);
			oneOf(meth).arrayItem(J.OBJECT, args, 1); will(returnValue(dummy));
		}});

		ArgSlot a0 = new ArgSlot(0, new HSIPatternOptions());
		ArgSlot a1 = new ArgSlot(1, new HSIPatternOptions());
		sv.hsiArgs(Arrays.asList(a0, a1));
		
		sv.switchOn(a0);
		sv.withConstructor("Nil");
		
		IExpr ass2 = context.mock(IExpr.class, "ass2");
		CaptureAction captureHead1 = new CaptureAction(ass2);
		context.checking(new Expectations() {{
			oneOf(meth).nextLocal(); will(returnValue(26));
			oneOf(meth).callInterface(J.OBJECT, cxt, "head", dummy); will(returnValue(dummy));
			oneOf(meth).assign(with(VarMatcher.local(26)), with(dummy)); will(captureHead1);
		}});
		sv.switchOn(a1);
		sv.withConstructor("Nil");
		
		FunctionIntro intro = new FunctionIntro(name, new ArrayList<>());
		StringLiteral expr = new StringLiteral(pos, "hello");
		FunctionCaseDefn fcd = new FunctionCaseDefn(null, expr);
		intro.functionCase(fcd);

		IExpr jvmExpr = context.mock(IExpr.class, "jvmExpr");
		context.checking(new Expectations() {{
			oneOf(meth).stringConst("hello"); will(returnValue(dummy));
			oneOf(meth).returnObject(dummy); will(returnValue(jvmExpr));
		}});
		sv.startInline(intro);
		sv.visitCase(fcd);
		sv.visitExpr(expr, 0);
		sv.visitStringLiteral(expr);
		sv.leaveCase(fcd);
		sv.endInline(intro);
		
		IExpr nscInner = context.mock(IExpr.class, "nscInner");
		context.checking(new Expectations() {{
			oneOf(meth).stringConst("no matching case"); will(returnValue(dummy));
			oneOf(meth).arrayOf(J.OBJECT, dummy); will(returnValue(dummy));
			oneOf(meth).callStatic(J.FLERROR, J.OBJECT, "eval", cxt, dummy); will(returnValue(dummy));
			oneOf(meth).returnObject(dummy); will(returnValue(nscInner));
		}});
		sv.defaultCase();
		sv.errorNoCase();

		Var head1 = (Var) captureHead1.get(0);
		IExpr innerIf = context.mock(IExpr.class, "innerIf");
		context.checking(new Expectations() {{
			oneOf(meth).stringConst("Nil"); will(returnValue(dummy));
			oneOf(meth).callInterface(JavaType.boolean_.toString(), cxt, "isA", head1, dummy); will(returnValue(dummy));
			oneOf(meth).ifBoolean(dummy, jvmExpr, nscInner); will(returnValue(innerIf));
		}});
		sv.endSwitch();
		
		IExpr nscOuter = context.mock(IExpr.class, "nscOuter");
		context.checking(new Expectations() {{
			oneOf(meth).stringConst("no matching case"); will(returnValue(dummy));
			oneOf(meth).arrayOf(J.OBJECT, dummy); will(returnValue(dummy));
			oneOf(meth).callStatic(J.FLERROR, J.OBJECT, "eval", cxt, dummy); will(returnValue(dummy));
			oneOf(meth).returnObject(dummy); will(returnValue(nscOuter));
		}});
		sv.defaultCase();
		sv.errorNoCase();

		Var head0 = (Var) captureHead0.get(0);
		IExpr ifExpr = context.mock(IExpr.class, "ifExpr");
		IExpr blk1 = context.mock(IExpr.class, "blk1");
		context.checking(new Expectations() {{
			oneOf(meth).stringConst("Nil"); will(returnValue(dummy));
			oneOf(meth).callInterface(JavaType.boolean_.toString(), cxt, "isA", head0, dummy); will(returnValue(dummy));
			oneOf(meth).block(ass2, innerIf); will(returnValue(blk1));
			oneOf(meth).ifBoolean(dummy, blk1, nscOuter); will(returnValue(ifExpr));
		}});
		sv.endSwitch();
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void stateIsCleanedUpBetweenFunctions() {
		IExpr head = context.mock(IExpr.class, "head");
		IExpr nsc = context.mock(IExpr.class, "nsc");
		IExpr ansc = context.mock(IExpr.class, "ansc");
		IExpr eNSC = context.mock(IExpr.class, "ensc");
		IExpr rerr = context.mock(IExpr.class, "rerr");
		IExpr doif = context.mock(IExpr.class, "doif");
		IExpr nil = context.mock(IExpr.class, "nil");
		IExpr isA = context.mock(IExpr.class, "isA");
		IExpr sret = context.mock(IExpr.class, "sret");
		IExpr re = context.mock(IExpr.class, "re");
		context.checking(new Expectations() {{
			oneOf(meth).nextLocal(); will(returnValue(22));
			oneOf(meth).nextLocal(); will(returnValue(23));
		}});
		Var cxt = new Var.AVar(meth, J.FLEVALCONTEXT, "cxt");
		Var args = new Var.AVar(meth, "[Object", "args");
		IExpr arg0 = context.mock(IExpr.class, "arg0");
		IExpr ass1 = context.mock(IExpr.class, "ass1");
		IExpr ass2 = context.mock(IExpr.class, "ass2");
		IExpr blk1 = context.mock(IExpr.class, "blk1");
		IExpr blk2 = context.mock(IExpr.class, "blk2");
		context.checking(new Expectations() {{
			oneOf(bce).newClass("test.repo.PACKAGEFUNCTIONS$x"); will(returnValue(bcc));
			oneOf(bcc).defineField(true, Access.PUBLICSTATIC, JavaType.int_, "nfargs");
			oneOf(bcc).createMethod(true, "java.lang.Object", "eval"); will(returnValue(meth));
			oneOf(meth).argument(J.FLEVALCONTEXT, "cxt"); will(returnValue(cxt));
			oneOf(meth).argument("[java.lang.Object", "args"); will(returnValue(args));

			oneOf(meth).nextLocal(); will(returnValue(25));
			oneOf(meth).arrayItem(J.OBJECT, args, 0); will(returnValue(arg0));
			oneOf(meth).callInterface(J.OBJECT, cxt, "head", arg0); will(returnValue(head));
			oneOf(meth).assign(with(VarMatcher.local(25)), with(head)); will(returnValue(ass1));
			oneOf(meth).stringConst("hello"); will(returnValue(sret));
			oneOf(meth).returnObject(sret); will(returnValue(re));
			
			oneOf(meth).stringConst("no matching case"); will(returnValue(nsc));
			oneOf(meth).arrayOf(J.OBJECT, nsc); will (returnValue(ansc));
			oneOf(meth).callStatic(J.FLERROR, J.OBJECT, "eval", cxt, ansc); will(returnValue(eNSC));
			oneOf(meth).returnObject(eNSC); will(returnValue(rerr));
			
			oneOf(meth).stringConst("Nil"); will(returnValue(nil));
			oneOf(meth).callInterface(with(JavaType.boolean_.toString()), with(cxt), with("isA"), with(Matchers.array(VarMatcher.local(25), Matchers.is(nil)))); will(returnValue(isA));
			
			oneOf(meth).ifBoolean(isA, re, rerr); will(returnValue(doif));
			oneOf(meth).block(ass1, doif); will(returnValue(blk1));
			oneOf(blk1).flush();

			oneOf(bce).newClass("test.repo.PACKAGEFUNCTIONS$y"); will(returnValue(bcc));
			oneOf(bcc).defineField(true, Access.PUBLICSTATIC, JavaType.int_, "nfargs");
			oneOf(bcc).createMethod(true, "java.lang.Object", "eval"); will(returnValue(meth));
			oneOf(meth).argument(J.FLEVALCONTEXT, "cxt"); will(returnValue(cxt));
			oneOf(meth).argument("[java.lang.Object", "args"); will(returnValue(args));
			
			oneOf(meth).nextLocal(); will(returnValue(25));
			oneOf(meth).arrayItem(J.OBJECT, args, 0); will(returnValue(arg0));
			oneOf(meth).callInterface(J.OBJECT, cxt, "head", arg0); will(returnValue(head));
			oneOf(meth).assign(with(VarMatcher.local(25)), with(head)); will(returnValue(ass2));
			oneOf(meth).stringConst("hello"); will(returnValue(sret));
			oneOf(meth).returnObject(sret); will(returnValue(re));
			
			oneOf(meth).stringConst("no matching case"); will(returnValue(nsc));
			oneOf(meth).arrayOf(J.OBJECT, nsc); will (returnValue(ansc));
			oneOf(meth).callStatic(J.FLERROR, J.OBJECT, "eval", cxt, ansc); will(returnValue(eNSC));
			oneOf(meth).returnObject(eNSC); will(returnValue(rerr));
			
			oneOf(meth).stringConst("Nil"); will(returnValue(nil));
			oneOf(meth).callInterface(with(JavaType.boolean_.toString()), with(cxt), with("isA"), with(Matchers.array(VarMatcher.local(25), Matchers.is(nil)))); will(returnValue(isA));
			
			oneOf(meth).ifBoolean(isA, re, rerr); will(returnValue(doif));
			oneOf(meth).block(ass2, doif); will(returnValue(blk2));
			oneOf(blk2).flush();
		}});
		StackVisitor sv = new StackVisitor();
		new JVMGenerator(bce, sv);
		{
			FunctionName name = FunctionName.function(pos, pkg, "x");
			FunctionDefinition fn = new FunctionDefinition(name, 1);
			FunctionIntro fi = new FunctionIntro(name, new ArrayList<>());
			FunctionCaseDefn fcd = new FunctionCaseDefn(null, new StringLiteral(pos, "hello"));
			fi.functionCase(fcd);
			fn.intro(fi);
			HSIArgsTree hsi = new HSIArgsTree(1);
			hsi.consider(fi);
			hsi.get(0).requireCM(LoadBuiltins.nil).consider(fi);
			fn.bindHsi(hsi);
			new Traverser(sv).withHSI().visitFunction(fn);
		}
		{
			FunctionName name = FunctionName.function(pos, pkg, "y");
			FunctionDefinition fn = new FunctionDefinition(name, 1);
			FunctionIntro fi = new FunctionIntro(name, new ArrayList<>());
			FunctionCaseDefn fcd = new FunctionCaseDefn(null, new StringLiteral(pos, "hello"));
			fi.functionCase(fcd);
			fn.intro(fi);
			HSIArgsTree hsi = new HSIArgsTree(1);
			hsi.consider(fi);
			hsi.get(0).requireCM(LoadBuiltins.nil).consider(fi);
			fn.bindHsi(hsi);
			new Traverser(sv).withHSI().visitFunction(fn);
		}
	}

}
