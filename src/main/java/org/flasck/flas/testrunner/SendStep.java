package org.flasck.flas.testrunner;

import java.util.List;

public class SendStep implements TestStep {
	private final String cardVar;
	private final String contractName;
	private final String methodName;
	private final List<Integer> args;
	private final List<Expectation> expects;

	public SendStep(String cardVar, String contractName, String methodName, List<Integer> args, List<Expectation> expects) {
		this.cardVar = cardVar;
		this.contractName = contractName;
		this.methodName = methodName;
		this.args = args;
		this.expects = expects;
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public void run(TestRunner runner) throws Exception {
		for (Expectation e : expects)
			runner.expect(cardVar, e.contract, e.method, (List)e.args);
		runner.send(cardVar, contractName, methodName, args);
	}

}
