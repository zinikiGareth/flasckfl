package test.flas.generator.jvm;

import java.util.ArrayList;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.NumericLiteral;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.commonBase.names.SolidName;
import org.flasck.flas.compiler.JVMGenerator;
import org.flasck.flas.parsedForm.ContractDecl;
import org.flasck.flas.parsedForm.FunctionCaseDefn;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.repository.Traverser;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Rule;
import org.junit.Test;
import org.zinutils.bytecode.ByteCodeSink;
import org.zinutils.bytecode.ByteCodeStorage;
import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.MethodDefiner;

public class ContractGeneration {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	private InputPosition pos = new InputPosition("-", 1, 0, null);
	private final PackageName pkg = new PackageName("test.repo");

	@Test
	public void simpleContractDeclarationForcesThreeClassesToBeGenerated() {
		ByteCodeStorage bce = context.mock(ByteCodeStorage.class);
		ByteCodeSink parent = context.mock(ByteCodeSink.class, "parent");
		ByteCodeSink up = context.mock(ByteCodeSink.class, "up");
		ByteCodeSink down = context.mock(ByteCodeSink.class, "down");
		MethodDefiner meth = context.mock(MethodDefiner.class);
//		IExpr iret = context.mock(IExpr.class, "ret");
//		IExpr nret = context.mock(IExpr.class, "nret");
//		IExpr nullVal = context.mock(IExpr.class, "null");
//		IExpr biv = context.mock(IExpr.class, "biv");
//		IExpr cdv = context.mock(IExpr.class, "cdv");
////		Var arg = new Var.AVar(meth, "JVMRunner", "runner");
		context.checking(new Expectations() {{
			oneOf(bce).newClass("test.repo.MyContract"); will(returnValue(parent));
			oneOf(bce).newClass("test.repo.MyContract$Down"); will(returnValue(parent));
			oneOf(bce).newClass("test.repo.MyContract$Up"); will(returnValue(parent));
//			oneOf(bcc).createMethod(true, "java.lang.Object", "eval"); will(returnValue(meth));
//			
//			// TODO: should this have a "FLContext" argument (or whatever?)
////			oneOf(meth).argument("org.flasck.flas.testrunner.JVMRunner", "runner"); will(returnValue(arg));
//			oneOf(meth).intConst(42); will(returnValue(iret));
//			oneOf(meth).aNull(); will(returnValue(nullVal));
//			oneOf(meth).box(iret); will(returnValue(biv));
//			oneOf(meth).castTo(nullVal, "java.lang.Double"); will(returnValue(cdv));
//			oneOf(meth).makeNew("org.flasck.jvm.builtin.FLNumber", biv, cdv); will(returnValue(nret));
//			oneOf(meth).returnObject(nret);
		}});
		JVMGenerator gen = new JVMGenerator(bce);
		SolidName cname = new SolidName(pkg, "MyContract");
		ContractDecl cd = new ContractDecl(pos, pos, cname);
		new Traverser(gen).visitContractDecl(cd);
	}
}
