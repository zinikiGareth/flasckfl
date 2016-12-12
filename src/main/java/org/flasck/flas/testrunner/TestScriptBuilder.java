package org.flasck.flas.testrunner;

import org.flasck.flas.blockForm.InputPosition;

public interface TestScriptBuilder {
	void error(String msg);

	void addAssert(InputPosition evalPos, Object eval, InputPosition pos, Object valueExpr);
	// TODO: other steps as well

	void addTestCase(String message);

}
