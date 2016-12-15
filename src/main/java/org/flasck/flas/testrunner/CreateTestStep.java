package org.flasck.flas.testrunner;

import org.flasck.flas.blockForm.InputPosition;

public class CreateTestStep implements TestStep {

	private final String bindVar;
	private final String cardType;

	public CreateTestStep(InputPosition at, String bindVar, String cardType) {
		this.bindVar = bindVar;
		this.cardType = cardType;
	}

	@Override
	public void run(TestRunner runner) throws Exception {
		runner.createCardAs(cardType, bindVar);
	}
}
