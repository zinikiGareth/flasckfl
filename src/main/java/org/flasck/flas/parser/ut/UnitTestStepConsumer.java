package org.flasck.flas.parser.ut;

import java.util.List;

import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.TargetZone;
import org.flasck.flas.parsedForm.TemplateReference;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.parsedForm.ut.MatchedItem;
import org.flasck.flas.parsedForm.ut.UnitTestStep;

public interface UnitTestStepConsumer {
	void assertion(Expr expr, Expr value);
	void closeCard(UnresolvedVar card);
	void shove(List<UnresolvedVar> slots, Expr value);
	void data(ErrorReporter errors, UnitDataDeclaration dd);
	void render(UnresolvedVar unresolvedVar, TemplateReference template);
	void event(UnresolvedVar card, TargetZone targetZone, Expr event);
	void input(UnresolvedVar card, TargetZone targetZone, Expr text);
	void invokeObjectMethod(Expr expr);
	void sendOnContract(UnresolvedVar card, TypeReference contract, Expr invocation);
	void expect(UnresolvedVar ctr, UnresolvedVar meth, Expr[] args, Expr handler);
	void expectCancel(UnresolvedVar handlerName);
	void match(UnresolvedVar card, MatchedItem what, TargetZone targetZone, boolean contains, boolean fails, String text);
	void newdiv(Integer cnt);
	void other(UnitTestStep step);
}
