package org.flasck.flas.parsedForm.ut;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.TargetZone;
import org.flasck.flas.parsedForm.TemplateReference;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.parser.ut.UnitDataDeclaration;
import org.flasck.flas.parser.ut.UnitTestStepConsumer;

public class TestStepHolder implements UnitTestStepConsumer {
	public final List<UnitTestStep> steps = new ArrayList<>();

	@Override
	public void assertion(Expr expr, Expr value) {
		this.steps.add(new UnitTestAssert(expr, value));
	}
	
	@Override
	public void closeCard(UnresolvedVar card) {
		this.steps.add(new UnitTestClose(card));
	}

	@Override
	public void shove(List<UnresolvedVar> slots, Expr value) {
		this.steps.add(new UnitTestShove(slots, value));
	}

	@Override
	public void data(ErrorReporter errors, UnitDataDeclaration dd) {
		this.steps.add(dd);
	}

	@Override
	public void render(UnresolvedVar obj, TemplateReference template) {
		this.steps.add(new UnitTestRender(obj, template));
	}

	@Override
	public void event(UnresolvedVar card, TargetZone targetZone, Expr event) {
		this.steps.add(new UnitTestEvent(card, targetZone, event));
	}

	@Override
	public void input(UnresolvedVar card, TargetZone targetZone, Expr text) {
		this.steps.add(new UnitTestInput(card, targetZone, text));
	}

	@Override
	public void invokeObjectMethod(Expr expr) {
		this.steps.add(new UnitTestInvoke(expr));
	}

	@Override
	public void sendOnContract(UnresolvedVar card, TypeReference contract, Expr invocation) {
		this.steps.add(new UnitTestSend(card, contract, invocation));
	}

	@Override
	public void expect(UnresolvedVar ctr, UnresolvedVar meth, Expr[] args, Expr handler) {
		this.steps.add(new UnitTestExpect(ctr, meth, args, handler));
	}

	@Override
	public void expectCancel(UnresolvedVar handlerName) {
		this.steps.add(new UnitTestExpectCancel(handlerName));
	}

	@Override
	public void match(UnresolvedVar card, MatchedItem what, TargetZone targetZone, boolean contains, boolean fails, String text) {
		this.steps.add(new UnitTestMatch(card, what, targetZone, contains, fails, text));
	}

	@Override
	public void newdiv(Integer cnt) {
		this.steps.add(new UnitTestNewDiv(cnt));
	}

	@Override
	public void other(UnitTestStep step) {
		this.steps.add(step);
	}
}
