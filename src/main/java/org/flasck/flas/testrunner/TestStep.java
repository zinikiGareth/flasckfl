package org.flasck.flas.testrunner;

import org.zinutils.bytecode.BCEClassLoader;

public interface TestStep {

	void run(BCEClassLoader loader, String scriptPkg) throws AssertFailed, ClassNotFoundException;

}
