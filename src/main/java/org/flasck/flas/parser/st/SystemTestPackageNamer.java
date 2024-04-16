package org.flasck.flas.parser.st;

import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.commonBase.names.SystemTestName;

public class SystemTestPackageNamer implements SystemTestNamer {
	private final PackageName file;
	private int step = 0;

	public SystemTestPackageNamer(PackageName fn) {
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
