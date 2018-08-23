package org.flasck.flas.testrunner;

public interface UnitTestResultHandler {

	void testPassed(String caseName, String runner);

	void testFailed(String caseName, String runner, Object expected, Object actual);

	void testError(String description, String runner, Exception ex);

	void testError(String description, String runner, String msg);

}
