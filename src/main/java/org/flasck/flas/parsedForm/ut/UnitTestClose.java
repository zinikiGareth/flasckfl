package org.flasck.flas.parsedForm.ut;

import org.flasck.flas.parsedForm.UnresolvedVar;

public class UnitTestClose implements UnitTestStep {
	public final UnresolvedVar card;

	public UnitTestClose(UnresolvedVar card) {
		this.card = card;
	}
}
