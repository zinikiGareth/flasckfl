package org.flasck.flas.parsedForm.ut;

import org.flasck.flas.parsedForm.UnresolvedVar;

public class UnitTestExpectCancel implements UnitTestStep {
	public final UnresolvedVar handlerName;

	public UnitTestExpectCancel(UnresolvedVar handlerName) {
		this.handlerName = handlerName;
	}
}
