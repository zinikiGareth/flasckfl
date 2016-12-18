package org.flasck.flas.testrunner;

public class SendStep implements TestStep {
	private final String cardVar;
	private final String contractName;
	private final String methodName;

	public SendStep(String cardVar, String contractName, String methodName) {
		this.cardVar = cardVar;
		this.contractName = contractName;
		this.methodName = methodName;
	}
	
	@Override
	public void run(TestRunner runner) throws Exception {
		runner.send(cardVar, contractName, methodName);
	}

}
