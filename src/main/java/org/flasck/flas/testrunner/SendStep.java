package org.flasck.flas.testrunner;

import java.util.List;

public class SendStep implements TestStep {
	private final String cardVar;
	private final String contractName;
	private final String methodName;
	private final List<Integer> args;

	public SendStep(String cardVar, String contractName, String methodName, List<Integer> args) {
		this.cardVar = cardVar;
		this.contractName = contractName;
		this.methodName = methodName;
		this.args = args;
	}
	
	@Override
	public void run(TestRunner runner) throws Exception {
		runner.send(cardVar, contractName, methodName, args);
	}

}
