package test.flas.generator.jvm;

import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.commonBase.names.UnitTestFileName;
import org.flasck.flas.commonBase.names.UnitTestName;
import org.flasck.flas.compiler.JVMGenerator;
import org.flasck.flas.parsedForm.ut.UnitTestCase;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Rule;
import org.junit.Test;
import org.zinutils.bytecode.ByteCodeSink;
import org.zinutils.bytecode.ByteCodeStorage;
import org.zinutils.bytecode.MethodDefiner;

public class UnitTestGeneration {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();

	@Test
	public void weDoActuallyCreateATestCaseClass() {
		ByteCodeStorage bce = context.mock(ByteCodeStorage.class);
		ByteCodeSink bcc = context.mock(ByteCodeSink.class);
		MethodDefiner meth = context.mock(MethodDefiner.class);
		context.checking(new Expectations() {{
			oneOf(bce).newClass("test.something._ut_package._ut4"); will(returnValue(bcc));
			oneOf(bcc).createMethod(true, "void", "dotest"); will(returnValue(meth));
			oneOf(meth).argument("org.flasck.flas.testrunner.JVMRunner", "runner");
			oneOf(meth).returnVoid();
			oneOf(bcc).generate();
		}});
		JVMGenerator gen = new JVMGenerator(bce);
		UnitTestFileName utfn = new UnitTestFileName(new PackageName("test.something"), "_ut_package");
		UnitTestName utn = new UnitTestName(utfn, 4);
		UnitTestCase utc = new UnitTestCase(utn , "do something");
		gen.visitUnitTest(utc);
	}
}
