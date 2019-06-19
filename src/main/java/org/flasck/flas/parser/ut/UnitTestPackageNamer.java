package org.flasck.flas.parser.ut;

import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.commonBase.names.UnitTestName;

public class UnitTestPackageNamer extends TestStepNamer implements UnitTestNamer {
	private int which = 0;

	public UnitTestPackageNamer(String inPkg, String file) {
		super(new PackageName(inPkg + "._ut_" + file));
	}

	@Override
	public UnitTestName unitTest() {
		return new UnitTestName(name, which++);
	}
}
