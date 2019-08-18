package test.flas.generator.jvm;

import java.util.ArrayList;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.NumericLiteral;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.compiler.JVMGenerator;
import org.flasck.flas.parsedForm.FunctionCaseDefn;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.repository.Traverser;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.zinutils.bytecode.ByteCodeSink;
import org.zinutils.bytecode.ByteCodeStorage;
import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.MethodDefiner;

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
//		Var arg = new Var.AVar(meth, "JVMRunner", "runner");
		context.checking(new Expectations() {{
			oneOf(bce).newClass("test.repo.PACKAGEFUNCTIONS$x"); will(returnValue(bcc));
			oneOf(bcc).createMethod(true, "java.lang.Object", "eval"); will(returnValue(meth));
			
			// TODO: should this have a "FLContext" argument (or whatever?)
//			oneOf(meth).argument("org.flasck.flas.testrunner.JVMRunner", "runner"); will(returnValue(arg));
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
		new Traverser(gen).visitFunction(fn);
	}
}
