package org.flasck.flas.parser.ut;

import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.parsedForm.UnresolvedVar;

public interface UnitTestStepConsumer {
	void assertion(Expr expr, Expr value);
	void data(UnitDataDeclaration dd);
	void event(UnresolvedVar card, Expr event);
	void invokeObjectMethod(Expr expr);
	void sendOnContract(UnresolvedVar card, TypeReference contract, Expr invocation);
	void expect(UnresolvedVar ctr, UnresolvedVar meth, Expr[] args, Expr handler);
	void template();
}
