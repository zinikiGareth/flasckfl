package org.flasck.flas.testrunner;

public interface TestScriptBuilder {

	void add(SingleTestCase test);

	void error(String msg);

	void add(AssertTestStep assertTestStep);

	void addTestCase(String message);

}
