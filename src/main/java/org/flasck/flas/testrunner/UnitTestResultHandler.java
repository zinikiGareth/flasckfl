package org.flasck.flas.testrunner;

public interface UnitTestResultHandler {

	void testPassed(String caseName);

	void testFailed(String caseName, Object expected, Object actual);

	void testError(String description, Exception ex);

}
