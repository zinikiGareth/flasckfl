package org.flasck.flas.testrunner;

public class SendStep implements TestStep {

	@Override
	public void run(TestRunner runner) throws Exception {
		runner.send();
	}

}
