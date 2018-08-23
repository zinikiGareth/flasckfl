package org.flasck.flas.testrunner;

import org.zinutils.utils.MultiTextEmitter;

public class FileUnitTestResultHandler implements UnitTestResultHandler {
	private final MultiTextEmitter results;

	public FileUnitTestResultHandler(MultiTextEmitter results) {
		this.results = results;
	}

	@Override
	public void testPassed(String caseName, String runner) {
		results.println("PASSED: " + caseName + " (" + runner + ")");
	}

	@Override
	public void testFailed(String caseName, String runner, Object expected, Object actual) {
		results.println("FAILED: " + caseName + " (" + runner + ")");
	}

	@Override
	public void testError(String caseName, String runner, Exception ex) {
		results.println("ERROR:  " + caseName + " (" + runner + ")");
		results.showException(ex);
	}

	@Override
	public void testError(String caseName, String runner, String s) {
		results.println("ERROR:  " + caseName);
		results.println("  " + s);
	}
}
