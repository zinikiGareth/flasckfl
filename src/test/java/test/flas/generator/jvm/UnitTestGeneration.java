package test.flas.generator.jvm;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.NumericLiteral;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.commonBase.names.UnitTestFileName;
import org.flasck.flas.commonBase.names.UnitTestName;
import org.flasck.flas.compiler.JVMGenerator;
import org.flasck.flas.parsedForm.ut.UnitTestAssert;
import org.flasck.flas.parsedForm.ut.UnitTestCase;
import org.flasck.flas.repository.Traverser;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Rule;
import org.junit.Test;
import org.zinutils.bytecode.ByteCodeSink;
import org.zinutils.bytecode.ByteCodeStorage;
import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.MethodDefiner;
import org.zinutils.bytecode.Var;

public class UnitTestGeneration {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	private InputPosition pos = new InputPosition("-", 1, 0, null);

	@Test
	public void weDoActuallyCreateATestCaseClass() {
		ByteCodeStorage bce = context.mock(ByteCodeStorage.class);
		ByteCodeSink bcc = context.mock(ByteCodeSink.class);
		MethodDefiner meth = context.mock(MethodDefiner.class);
		context.checking(new Expectations() {{
			oneOf(meth).nextLocal(); will(returnValue(6));
		}});
		Var arg = new Var.AVar(meth, "JVMRunner", "runner");
		context.checking(new Expectations() {{
			oneOf(bce).newClass("test.something._ut_package._ut4"); will(returnValue(bcc));
			oneOf(bcc).createMethod(true, "void", "dotest"); will(returnValue(meth));
			oneOf(meth).argument("org.flasck.flas.testrunner.JVMRunner", "runner"); will(returnValue(arg));
		}});
		JVMGenerator gen = new JVMGenerator(bce);
		UnitTestFileName utfn = new UnitTestFileName(new PackageName("test.something"), "_ut_package");
		UnitTestName utn = new UnitTestName(utfn, 4);
		UnitTestCase utc = new UnitTestCase(utn , "do something");
		gen.visitUnitTest(utc);
		context.checking(new Expectations() {{
			oneOf(meth).returnVoid();
			oneOf(bcc).generate();
		}});
		gen.leaveUnitTest(utc);
	}
	
	@Test
	public void weVisitAnAssertStep() {
		MethodDefiner meth = context.mock(MethodDefiner.class);
		NumericLiteral lhs = new NumericLiteral(pos, 42);
		StringLiteral rhs = new StringLiteral(pos, "hello");
		IExpr runner = null;
		IExpr l1 = null;
		IExpr r1 = null;
		context.checking(new Expectations() {{
			oneOf(meth).intConst(42); will(returnValue(l1));
			oneOf(meth).stringConst("hello"); will(returnValue(r1));
			oneOf(meth).callVirtual("void", runner, "assertSameValue", l1, r1);
		}});
		Traverser gen = new Traverser(JVMGenerator.forTests(meth));
		UnitTestAssert a = new UnitTestAssert(lhs, rhs);
		gen.visitUnitTestAssert(a);
	}
}
