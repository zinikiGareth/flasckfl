package org.flasck.flas.testrunner;

public interface TestScriptBuilder {

	void add(TestCase test);

	void error(String msg);

}
