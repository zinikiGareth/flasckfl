package org.flasck.flas.parsedForm.ut;

import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.parser.ut.UnitTestStepConsumer;

public class UnitTestCase implements UnitTestStepConsumer {
	public final String description;

	public UnitTestCase(String description) {
		this.description = description;
	}

	@Override
	public void assertion(Expr expr, Expr value) {
	}

	@Override
	public void event(UnresolvedVar card, StringLiteral name, Expr event) {
	}

	@Override
	public void send(UnresolvedVar card, TypeReference contract, Expr invocation) {
	}
}
