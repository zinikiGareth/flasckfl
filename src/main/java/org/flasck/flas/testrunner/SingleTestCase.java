package org.flasck.flas.testrunner;

import java.util.ArrayList;
import java.util.List;

import org.zinutils.bytecode.BCEClassLoader;
import org.zinutils.exceptions.UtilException;

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
	
	public void run(BCEClassLoader loader, String scriptPkg) throws Exception {
		for (TestStep s : steps)
			s.run(loader, scriptPkg);
	}

	public void assertStepCount(int quant) {
		if (steps == null || steps.size() != quant)
			throw new UtilException("Did not have " + quant + " steps, but " + steps.size());
	}
}
