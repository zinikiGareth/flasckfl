package org.flasck.flas.parser.st;

import org.flasck.flas.commonBase.names.SystemTestName;
import org.flasck.flas.commonBase.names.UnitTestFileName;

public class SystemTestPackageNamer implements SystemTestNamer {
	private final UnitTestFileName file;
	private int step = 0;

	public SystemTestPackageNamer(UnitTestFileName fn) {
		this.file = fn;
	}

	@Override
	public SystemTestName special(String name) {
		return new SystemTestName(file, name);
	}

	@Override
	public SystemTestName nextStep() {
		return new SystemTestName(file, step++);
	}
}
