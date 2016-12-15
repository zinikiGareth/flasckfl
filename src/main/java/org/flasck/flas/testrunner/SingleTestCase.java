package org.flasck.flas.testrunner;

import java.util.ArrayList;
import java.util.List;

public class SingleTestCase {
	private final String message;
	private final List<TestStep> steps;

	public SingleTestCase(String message, List<TestStep> steps) {
		this.message = message;
		this.steps = new ArrayList<TestStep>(steps);
	}

	public String getDescription() {
		return message;
	}
	
	public void run(TestRunner runner) throws Exception {
		for (TestStep s : steps)
			s.run(runner);
	}
}
