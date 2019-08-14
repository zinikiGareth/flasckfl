package org.flasck.flas.parser.ut;

import org.flasck.flas.commonBase.names.UnitTestFileName;
import org.flasck.flas.commonBase.names.UnitTestName;

public class UnitTestPackageNamer extends TestStepNamer implements UnitTestNamer {
	private int which = 0;

	public UnitTestPackageNamer(UnitTestFileName fn) {
		super(fn);
	}

	@Override
	public UnitTestName unitTest() {
		return new UnitTestName((UnitTestFileName) name, which++);
	}
}
