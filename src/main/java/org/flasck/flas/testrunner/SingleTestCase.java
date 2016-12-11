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

	public String description() {
		return message;
	}

	public List<TestStep> steps() {
		return steps;
	}
}
