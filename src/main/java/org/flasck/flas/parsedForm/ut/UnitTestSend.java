package org.flasck.flas.parsedForm.ut;

import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.parsedForm.UnresolvedVar;

public class UnitTestSend implements UnitTestStep {
	public final UnresolvedVar card;
	public final TypeReference contract;
	public final Expr expr;
	
	public UnitTestSend(UnresolvedVar card, TypeReference contract, Expr expr) {
		this.card = card;
		this.contract = contract;
		this.expr = expr;
	}
}
