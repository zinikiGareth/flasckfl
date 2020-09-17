package org.flasck.flas.parser.st;

import org.flasck.flas.commonBase.names.SystemTestName;

public interface SystemTestNamer {
	SystemTestName special(String name);
	SystemTestName nextStep();
}
