package org.flasck.flas.parsedForm.ut;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.commonBase.names.UnitTestName;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.parser.ut.UnitDataDeclaration;
import org.flasck.flas.parser.ut.UnitTestStepConsumer;

public class UnitTestCase implements UnitTestStepConsumer {
	public final UnitTestName name;
	public final String description;
	public final List<UnitTestStep> steps = new ArrayList<>();

	public UnitTestCase(UnitTestName name, String description) {
		this.name = name;
		this.description = description;
	}

	@Override
	public void assertion(Expr expr, Expr value) {
		this.steps.add(new UnitTestAssert(expr, value));
	}

	@Override
	public void data(UnitDataDeclaration dd) {
		this.steps.add(dd);
	}

	@Override
	public void event(UnresolvedVar card, StringLiteral name, Expr event) {
		this.steps.add(new UnitTestEvent(card, name, event));
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
	public void expect(UnresolvedVar ctr, UnresolvedVar meth, Object... args) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void template() {
	}

	@Override
	public String toString() {
		return "UnitTestCase[" + description + "]";
	}
}
