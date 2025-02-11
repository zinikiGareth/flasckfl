package org.flasck.flas.testrunner;

import java.util.List;

public interface JSTestController {

	void ready();

	void stepsForTest(List<String> steps);

	void systemTestPrepared();

	void error(String string);

}
