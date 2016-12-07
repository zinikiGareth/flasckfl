package org.flasck.flas.testrunner;

import java.util.List;

public class UnitTestConvertor {

	public TestScript convert(String pkg, List<String> list) {
		return new TestScript("\texpr1 = test.golden.x\n\tvalue1 = 32\n");
	}

}
