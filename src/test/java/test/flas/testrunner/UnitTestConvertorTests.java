package test.flas.testrunner;

import static org.junit.Assert.*;

import org.flasck.flas.testrunner.TestScript;
import org.flasck.flas.testrunner.UnitTestConvertor;
import org.junit.Test;
import org.zinutils.collections.CollectionUtils;

public class UnitTestConvertorTests {

	@Test
	public void testSimpleVariable() {
		UnitTestConvertor ctor = new UnitTestConvertor();
		TestScript script = ctor.convert("test.golden", CollectionUtils.listOf("\t value x", "\t\t 32"));
		assertEquals("\texpr1 = test.golden.x\n\tvalue1 = 32\n", script.flas);
	}

}
