package org.flasck.flas.parser.ut;

import java.util.List;

import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.parsedForm.TargetZone;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.parsedForm.ut.MatchedItem;

public interface UnitTestStepConsumer {
	void assertion(Expr expr, Expr value);
	void shove(List<UnresolvedVar> slots, Expr value);
	void data(UnitDataDeclaration dd);
	void event(UnresolvedVar card, TargetZone targetZone, Expr event);
	void invokeObjectMethod(Expr expr);
	void sendOnContract(UnresolvedVar card, TypeReference contract, Expr invocation);
	void expect(UnresolvedVar ctr, UnresolvedVar meth, Expr[] args, Expr handler);
	void match(UnresolvedVar card, MatchedItem what, TargetZone targetZone, boolean contains, String text);
}
