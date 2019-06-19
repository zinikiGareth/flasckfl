package org.flasck.flas.parser.ut;

import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.parsedForm.UnresolvedVar;

public interface UnitTestStepConsumer {
	void assertion(Expr expr, Expr value);
	void event(UnresolvedVar card, StringLiteral name, Expr event);
	void send(UnresolvedVar card, TypeReference contract, Expr invocation);
	void data(UnitDataDeclaration dd);
	void template();
}
