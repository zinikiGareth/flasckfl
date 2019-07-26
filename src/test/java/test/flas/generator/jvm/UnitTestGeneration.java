package test.flas.generator.jvm;

import static org.junit.Assert.*;

import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.commonBase.names.UnitTestFileName;
import org.flasck.flas.commonBase.names.UnitTestName;
import org.flasck.flas.compiler.JVMGenerator;
import org.flasck.flas.parsedForm.ut.UnitTestCase;
import org.junit.Test;
import org.zinutils.bytecode.ByteCodeEnvironment;

public class UnitTestGeneration {

	@Test
	public void weDoActuallyCreateATestCaseClass() {
		ByteCodeEnvironment bce = new ByteCodeEnvironment();
		JVMGenerator gen = new JVMGenerator(bce);
		UnitTestFileName utfn = new UnitTestFileName(new PackageName("test.something"), "_ut_package");
		UnitTestName utn = new UnitTestName(utfn, 4);
		UnitTestCase utc = new UnitTestCase(utn , "do something");
		gen.visitUnitTest(utc);
		assertEquals(1, bce.size());
	}

}
