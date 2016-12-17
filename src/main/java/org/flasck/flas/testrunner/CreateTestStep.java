package org.flasck.flas.testrunner;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.CardName;

public class CreateTestStep implements TestStep {
	private final String bindVar;
	private final CardName cardType;

	public CreateTestStep(InputPosition at, String bindVar, CardName cardType) {
		this.bindVar = bindVar;
		this.cardType = cardType;
	}

	@Override
	public void run(TestRunner runner) throws Exception {
		runner.createCardAs(cardType, bindVar);
	}
}
