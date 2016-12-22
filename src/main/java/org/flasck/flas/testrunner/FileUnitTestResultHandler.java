package org.flasck.flas.testrunner;

import org.zinutils.utils.MultiTextEmitter;

public class FileUnitTestResultHandler implements UnitTestResultHandler {
	private final MultiTextEmitter results;

	public FileUnitTestResultHandler(MultiTextEmitter results) {
		this.results = results;
	}

	@Override
	public void testPassed(String caseName) {
		results.println("PASSED: " + caseName);
	}

	@Override
	public void testFailed(String caseName, Object expected, Object actual) {
		results.println("FAILED: " + caseName);
	}

	@Override
	public void testError(String caseName, Exception ex) {
		results.println("ERROR:  " + caseName);
		results.showException(ex);
	}

	@Override
	public void testError(String caseName, String s) {
		results.println("ERROR:  " + caseName);
		results.println("  " + s);
	}
}
