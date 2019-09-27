package test.flas.generator.jvm;

import java.util.ArrayList;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.NumericLiteral;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.compiler.JVMGenerator;
import org.flasck.flas.hsi.ArgSlot;
import org.flasck.flas.parsedForm.FunctionCaseDefn;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.patterns.HSIPatternOptions;
import org.flasck.flas.patterns.HSIPatternTree;
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
			allowing(meth).lenientMode(with(any(Boolean.class)));
		}});
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
		Var cxt = new Var.AVar(meth, "org.ziniki.ziwsh.json.FLEvalContext", "cxt");
		Var args = new Var.AVar(meth, "JVMRunner", "runner");
		context.checking(new Expectations() {{
			oneOf(bce).newClass("test.repo.PACKAGEFUNCTIONS$x"); will(returnValue(bcc));
			oneOf(bcc).createMethod(true, "java.lang.Object", "eval"); will(returnValue(meth));
			oneOf(meth).argument("org.ziniki.ziwsh.json.FLEvalContext", "cxt"); will(returnValue(cxt));
			oneOf(meth).argument("[java.lang.Object", "args"); will(returnValue(args));

			oneOf(meth).intConst(42); will(returnValue(iret));
			oneOf(meth).aNull(); will(returnValue(nullVal));
			oneOf(meth).box(iret); will(returnValue(biv));
			oneOf(meth).castTo(nullVal, "java.lang.Double"); will(returnValue(cdv));
			oneOf(meth).makeNew("org.flasck.jvm.builtin.FLNumber", biv, cdv); will(returnValue(nret));
			oneOf(meth).returnObject(nret); will(returnValue(re));
			oneOf(re).flush();
		}});
		JVMGenerator gen = new JVMGenerator(bce);
		FunctionName name = FunctionName.function(pos, pkg, "x");
		FunctionDefinition fn = new FunctionDefinition(name, 0);
		FunctionIntro fi = new FunctionIntro(name, new ArrayList<>());
		FunctionCaseDefn fcd = new FunctionCaseDefn(null, new NumericLiteral(pos, 42));
		fi.functionCase(fcd);
		fn.intro(fi);
		HSIPatternTree hsi = new HSIPatternTree(0);
		hsi.consider(fi);
		fn.bindHsi(hsi);
		new Traverser(gen).visitFunction(fn);
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
			oneOf(meth).nextLocal(); will(returnValue(24));
		}});
		Var cxt = new Var.AVar(meth, "org.ziniki.ziwsh.json.FLEvalContext", "cxt");
		Var args = new Var.AVar(meth, "[Object", "args");
		Var arg0 = new Var.AVar(meth, "Object", "arg");
		context.checking(new Expectations() {{
			oneOf(bce).newClass("test.repo.PACKAGEFUNCTIONS$x"); will(returnValue(bcc));
			oneOf(bcc).createMethod(true, "java.lang.Object", "eval"); will(returnValue(meth));
			oneOf(meth).argument("org.ziniki.ziwsh.json.FLEvalContext", "cxt"); will(returnValue(cxt));
			oneOf(meth).argument("[java.lang.Object", "args"); will(returnValue(args));

			oneOf(meth).nextLocal(); will(returnValue(25));
			oneOf(meth).arrayItem(J.OBJECT, args, 0); will(returnValue(arg0));
			oneOf(meth).callStatic(J.FLEVAL, J.OBJECT, "head", cxt, arg0); will(returnValue(head));
			oneOf(meth).assign(with(VarMatcher.local(25)), with(head));
			oneOf(meth).stringConst("hello"); will(returnValue(sret));
			oneOf(meth).returnObject(sret); will(returnValue(re));
			
			oneOf(meth).stringConst("no such case"); will(returnValue(nsc));
			oneOf(meth).arrayOf(J.OBJECT, nsc); will (returnValue(ansc));
			oneOf(meth).callStatic(J.ERROR, J.OBJECT, "eval", cxt, ansc); will(returnValue(eNSC));
			oneOf(meth).returnObject(eNSC); will(returnValue(rerr));
			
			oneOf(meth).stringConst("Nil"); will(returnValue(nil));
			oneOf(meth).callStatic(with(J.FLEVAL), with(JavaType.boolean_), with("isA"), with(Matchers.array(Matchers.is(cxt), VarMatcher.local(25), Matchers.is(nil)))); will(returnValue(isA));
			
			oneOf(meth).ifBoolean(isA, re, rerr); will(returnValue(doif));
			oneOf(doif).flush();
		}});
		JVMGenerator gen = new JVMGenerator(bce);
		FunctionName name = FunctionName.function(pos, pkg, "x");
		FunctionDefinition fn = new FunctionDefinition(name, 1);
		FunctionIntro fi = new FunctionIntro(name, new ArrayList<>());
		FunctionCaseDefn fcd = new FunctionCaseDefn(null, new StringLiteral(pos, "hello"));
		fi.functionCase(fcd);
		fn.intro(fi);
		HSIPatternTree hsi = new HSIPatternTree(1);
		hsi.consider(fi);
		hsi.get(0).requireCM("Nil", 0).consider(fi);
		fn.bindHsi(hsi);
		new Traverser(gen).visitFunction(fn);
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
			oneOf(meth).nextLocal(); will(returnValue(24));
		}});
		Var cxt = new Var.AVar(meth, "org.ziniki.ziwsh.json.FLEvalContext", "cxt");
		Var args = new Var.AVar(meth, "[Object", "args");
		Var arg0 = new Var.AVar(meth, "Object", "arg");
		context.checking(new Expectations() {{
			oneOf(bce).newClass("test.repo.PACKAGEFUNCTIONS$x"); will(returnValue(bcc));
			oneOf(bcc).createMethod(true, "java.lang.Object", "eval"); will(returnValue(meth));
			oneOf(meth).argument("org.ziniki.ziwsh.json.FLEvalContext", "cxt"); will(returnValue(cxt));
			oneOf(meth).argument("[java.lang.Object", "args"); will(returnValue(args));

			oneOf(meth).nextLocal(); will(returnValue(25));
			oneOf(meth).arrayItem(J.OBJECT, args, 0); will(returnValue(arg0));
			oneOf(meth).callStatic(J.FLEVAL, J.OBJECT, "head", cxt, arg0); will(returnValue(head));
			oneOf(meth).assign(with(VarMatcher.local(25)), with(head));
			
			oneOf(meth).stringConst("goodbye"); will(returnValue(gret));
			oneOf(meth).returnObject(gret); will(returnValue(gr));
			
			oneOf(meth).stringConst("hello"); will(returnValue(sret));
			oneOf(meth).returnObject(sret); will(returnValue(re));
			
			oneOf(meth).stringConst("no such case"); will(returnValue(nsc));
			oneOf(meth).arrayOf(J.OBJECT, nsc); will (returnValue(ansc));
			oneOf(meth).callStatic(J.ERROR, J.OBJECT, "eval", cxt, ansc); will(returnValue(eNSC));
			oneOf(meth).returnObject(eNSC); will(returnValue(rerr));
			
			oneOf(meth).stringConst("Nil"); will(returnValue(nil));
			oneOf(meth).callStatic(with(J.FLEVAL), with(JavaType.boolean_), with("isA"), with(Matchers.array(Matchers.is(cxt), VarMatcher.local(25), Matchers.is(nil)))); will(returnValue(isA));
			
			oneOf(meth).stringConst("Cons"); will(returnValue(cons));
			oneOf(meth).callStatic(with(J.FLEVAL), with(JavaType.boolean_), with("isA"), with(Matchers.array(Matchers.is(cxt), VarMatcher.local(25), Matchers.is(cons)))); will(returnValue(isACons));
			
			oneOf(meth).ifBoolean(isA, re, rerr); will(returnValue(doif));
			oneOf(meth).ifBoolean(isACons, gr, doif); will(returnValue(doif2));
			oneOf(doif2).flush();
		}});
		JVMGenerator gen = new JVMGenerator(bce);
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
		HSIPatternTree hsi = new HSIPatternTree(1);
		hsi.consider(f1);
		hsi.get(0).requireCM("Nil", 0).consider(f1);
		hsi.consider(f2);
		hsi.get(0).requireCM("Cons", 2).consider(f2);
		fn.bindHsi(hsi);
		new Traverser(gen).visitFunction(fn);
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
		
		IExpr ass1 = context.mock(IExpr.class, "ass1");
		CaptureAction captureHead0 = new CaptureAction(ass1);
		context.checking(new Expectations() {{
			oneOf(meth).arrayItem(J.OBJECT, args, 0); will(returnValue(dummy));
			oneOf(meth).nextLocal(); will(returnValue(25));
			oneOf(meth).callStatic(J.FLEVAL, J.OBJECT, "head", cxt, dummy); will(returnValue(dummy));
			oneOf(meth).assign(with(VarMatcher.local(25)), with(dummy)); will(captureHead0);
			oneOf(ass1).flush();
		}});
		ArgSlot a0 = new ArgSlot(0, new HSIPatternOptions());
		gen.switchOn(a0);
		gen.withConstructor("Nil");

		
		IExpr ass2 = context.mock(IExpr.class, "ass2");
		CaptureAction captureHead1 = new CaptureAction(ass2);
		context.checking(new Expectations() {{
			oneOf(meth).arrayItem(J.OBJECT, args, 1); will(returnValue(dummy));
			oneOf(meth).nextLocal(); will(returnValue(26));
			oneOf(meth).callStatic(J.FLEVAL, J.OBJECT, "head", cxt, dummy); will(returnValue(dummy));
			oneOf(meth).assign(with(VarMatcher.local(26)), with(dummy)); will(captureHead1);
			oneOf(ass2).flush();
		}});
		ArgSlot a1 = new ArgSlot(1, new HSIPatternOptions());
		gen.switchOn(a1);
		gen.withConstructor("Nil");
		
		FunctionName name = FunctionName.function(pos, pkg, "x");
		FunctionIntro intro = new FunctionIntro(name, new ArrayList<>());
		StringLiteral expr = new StringLiteral(pos, "hello");
		FunctionCaseDefn fcd = new FunctionCaseDefn(null, expr);
		intro.functionCase(fcd);

		IExpr jvmExpr = context.mock(IExpr.class, "jvmExpr");
		context.checking(new Expectations() {{
			oneOf(meth).stringConst("hello"); will(returnValue(dummy));
			oneOf(meth).returnObject(dummy); will(returnValue(jvmExpr));
		}});
		gen.startInline(intro);
		gen.visitExpr(expr, 0);
		gen.visitStringLiteral(expr);
		gen.endInline(intro);
		
		IExpr nscInner = context.mock(IExpr.class, "nscInner");
		context.checking(new Expectations() {{
			oneOf(meth).stringConst("no such case"); will(returnValue(dummy));
			oneOf(meth).arrayOf(J.OBJECT, dummy); will(returnValue(dummy));
			oneOf(meth).callStatic(J.ERROR, J.OBJECT, "eval", cxt, dummy); will(returnValue(dummy));
			oneOf(meth).returnObject(dummy); will(returnValue(nscInner));
		}});
		gen.errorNoCase();

		Var head1 = (Var) captureHead1.get(0);
		IExpr innerIf = context.mock(IExpr.class, "innerIf");
		context.checking(new Expectations() {{
			oneOf(meth).stringConst("Nil"); will(returnValue(dummy));
			oneOf(meth).callStatic(J.FLEVAL, JavaType.boolean_, "isA", cxt, head1, dummy); will(returnValue(dummy));
			oneOf(meth).ifBoolean(dummy, jvmExpr, nscInner); will(returnValue(innerIf));
		}});
		gen.endSwitch();
		
		IExpr nscOuter = context.mock(IExpr.class, "nscOuter");
		context.checking(new Expectations() {{
			oneOf(meth).stringConst("no such case"); will(returnValue(dummy));
			oneOf(meth).arrayOf(J.OBJECT, dummy); will(returnValue(dummy));
			oneOf(meth).callStatic(J.ERROR, J.OBJECT, "eval", cxt, dummy); will(returnValue(dummy));
			oneOf(meth).returnObject(dummy); will(returnValue(nscOuter));
		}});
		gen.errorNoCase();

		Var head0 = (Var) captureHead0.get(0);
		context.checking(new Expectations() {{
			oneOf(meth).stringConst("Nil"); will(returnValue(dummy));
			oneOf(meth).callStatic(J.FLEVAL, JavaType.boolean_, "isA", cxt, head0, dummy); will(returnValue(dummy));
			oneOf(meth).ifBoolean(dummy, innerIf, nscOuter);
		}});
		gen.endSwitch();
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
			oneOf(meth).nextLocal(); will(returnValue(24));
		}});
		Var cxt = new Var.AVar(meth, "org.ziniki.ziwsh.json.FLEvalContext", "cxt");
		Var args = new Var.AVar(meth, "[Object", "args");
		Var arg0 = new Var.AVar(meth, "Object", "arg");
		context.checking(new Expectations() {{
			oneOf(bce).newClass("test.repo.PACKAGEFUNCTIONS$x"); will(returnValue(bcc));
			oneOf(bcc).createMethod(true, "java.lang.Object", "eval"); will(returnValue(meth));
			oneOf(meth).argument("org.ziniki.ziwsh.json.FLEvalContext", "cxt"); will(returnValue(cxt));
			oneOf(meth).argument("[java.lang.Object", "args"); will(returnValue(args));

			oneOf(meth).nextLocal(); will(returnValue(25));
			oneOf(meth).arrayItem(J.OBJECT, args, 0); will(returnValue(arg0));
			oneOf(meth).callStatic(J.FLEVAL, J.OBJECT, "head", cxt, arg0); will(returnValue(head));
			oneOf(meth).assign(with(VarMatcher.local(25)), with(head));
			oneOf(meth).stringConst("hello"); will(returnValue(sret));
			oneOf(meth).returnObject(sret); will(returnValue(re));
			
			oneOf(meth).stringConst("no such case"); will(returnValue(nsc));
			oneOf(meth).arrayOf(J.OBJECT, nsc); will (returnValue(ansc));
			oneOf(meth).callStatic(J.ERROR, J.OBJECT, "eval", cxt, ansc); will(returnValue(eNSC));
			oneOf(meth).returnObject(eNSC); will(returnValue(rerr));
			
			oneOf(meth).stringConst("Nil"); will(returnValue(nil));
			oneOf(meth).callStatic(with(J.FLEVAL), with(JavaType.boolean_), with("isA"), with(Matchers.array(Matchers.is(cxt), VarMatcher.local(25), Matchers.is(nil)))); will(returnValue(isA));
			
			oneOf(meth).ifBoolean(isA, re, rerr); will(returnValue(doif));
			oneOf(doif).flush();

			oneOf(bce).newClass("test.repo.PACKAGEFUNCTIONS$y"); will(returnValue(bcc));
			oneOf(bcc).createMethod(true, "java.lang.Object", "eval"); will(returnValue(meth));
			oneOf(meth).argument("org.ziniki.ziwsh.json.FLEvalContext", "cxt"); will(returnValue(cxt));
			oneOf(meth).argument("[java.lang.Object", "args"); will(returnValue(args));
			
			oneOf(meth).nextLocal(); will(returnValue(25));
			oneOf(meth).arrayItem(J.OBJECT, args, 0); will(returnValue(arg0));
			oneOf(meth).callStatic(J.FLEVAL, J.OBJECT, "head", cxt, arg0); will(returnValue(head));
			oneOf(meth).assign(with(VarMatcher.local(25)), with(head));
			oneOf(meth).stringConst("hello"); will(returnValue(sret));
			oneOf(meth).returnObject(sret); will(returnValue(re));
			
			oneOf(meth).stringConst("no such case"); will(returnValue(nsc));
			oneOf(meth).arrayOf(J.OBJECT, nsc); will (returnValue(ansc));
			oneOf(meth).callStatic(J.ERROR, J.OBJECT, "eval", cxt, ansc); will(returnValue(eNSC));
			oneOf(meth).returnObject(eNSC); will(returnValue(rerr));
			
			oneOf(meth).stringConst("Nil"); will(returnValue(nil));
			oneOf(meth).callStatic(with(J.FLEVAL), with(JavaType.boolean_), with("isA"), with(Matchers.array(Matchers.is(cxt), VarMatcher.local(25), Matchers.is(nil)))); will(returnValue(isA));
			
			oneOf(meth).ifBoolean(isA, re, rerr); will(returnValue(doif));
			oneOf(doif).flush();
		}});
		JVMGenerator gen = new JVMGenerator(bce);
		{
			FunctionName name = FunctionName.function(pos, pkg, "x");
			FunctionDefinition fn = new FunctionDefinition(name, 1);
			FunctionIntro fi = new FunctionIntro(name, new ArrayList<>());
			FunctionCaseDefn fcd = new FunctionCaseDefn(null, new StringLiteral(pos, "hello"));
			fi.functionCase(fcd);
			fn.intro(fi);
			HSIPatternTree hsi = new HSIPatternTree(1);
			hsi.consider(fi);
			hsi.get(0).requireCM("Nil", 0).consider(fi);
			fn.bindHsi(hsi);
			new Traverser(gen).visitFunction(fn);
		}
		{
			FunctionName name = FunctionName.function(pos, pkg, "y");
			FunctionDefinition fn = new FunctionDefinition(name, 1);
			FunctionIntro fi = new FunctionIntro(name, new ArrayList<>());
			FunctionCaseDefn fcd = new FunctionCaseDefn(null, new StringLiteral(pos, "hello"));
			fi.functionCase(fcd);
			fn.intro(fi);
			HSIPatternTree hsi = new HSIPatternTree(1);
			hsi.consider(fi);
			hsi.get(0).requireCM("Nil", 0).consider(fi);
			fn.bindHsi(hsi);
			new Traverser(gen).visitFunction(fn);
		}
	}

}
