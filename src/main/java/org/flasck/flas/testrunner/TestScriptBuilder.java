package org.flasck.flas.testrunner;

public interface TestScriptBuilder {

	void error(String msg);

	void add(TestStep step);

	void addTestCase(String message);

}
