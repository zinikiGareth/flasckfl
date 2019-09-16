package test.flas.generator.jvm;

import java.util.ArrayList;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.NumericLiteral;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.compiler.JVMGenerator;
import org.flasck.flas.parsedForm.FunctionCaseDefn;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.FunctionIntro;
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
import org.zinutils.bytecode.Var.AVar;
import org.zinutils.bytecode.mock.VarMatcher;

public class FunctionGeneration {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	private InputPosition pos = new InputPosition("-", 1, 0, null);
	private final PackageName pkg = new PackageName("test.repo");
	private final MethodDefiner meth = context.mock(MethodDefiner.class);

	@Before
	public void setup() {
		context.checking(new Expectations() {{
			allowing(meth).lenientMode(with(any(Boolean.class)));
		}});
	}

	@Test
	public void aSimpleFunction() {
		ByteCodeStorage bce = context.mock(ByteCodeStorage.class);
		ByteCodeSink bcc = context.mock(ByteCodeSink.class);
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
		ByteCodeStorage bce = context.mock(ByteCodeStorage.class);
		ByteCodeSink bcc = context.mock(ByteCodeSink.class);
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
		hsi.get(0).addCM("Nil", new HSIPatternTree(0));
		fn.bindHsi(hsi);
		new Traverser(gen).visitFunction(fn);
	}

}
