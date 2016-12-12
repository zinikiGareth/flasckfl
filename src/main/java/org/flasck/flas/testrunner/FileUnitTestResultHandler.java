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
}
