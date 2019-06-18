package org.flasck.flas.parser.ut;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.commonBase.names.UnitTestName;

public class UnitTestPackageNamer implements UnitTestNamer {
	private final PackageName pkg;
	private int which = 0;

	public UnitTestPackageNamer(String inPkg, String file) {
		pkg = new PackageName(inPkg + "._ut_" + file);
	}

	@Override
	public UnitTestName unitTest() {
		return new UnitTestName(pkg, which++);
	}

	@Override
	public FunctionName dataName(InputPosition location, String text) {
		return FunctionName.function(location, pkg, text);
	}

}
