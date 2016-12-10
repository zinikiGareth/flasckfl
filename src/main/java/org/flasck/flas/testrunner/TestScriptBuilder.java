package org.flasck.flas.testrunner;

public interface TestScriptBuilder {
	void error(String msg);

	void add(AssertTestStep step);
	
	// TODO: other steps as well

	void addTestCase(String message);
}
