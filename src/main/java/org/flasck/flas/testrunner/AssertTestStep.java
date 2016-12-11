package org.flasck.flas.testrunner;

import org.flasck.flas.blockForm.InputPosition;

public class AssertTestStep implements TestStep {
	public final InputPosition evalPos;
	public final Object eval;
	public final InputPosition valuePos;
	public final Object value;
	private int exprId;

	public AssertTestStep(InputPosition evalPos, Object eval, InputPosition valuePos, Object value) {
		this.evalPos = evalPos;
		this.eval = eval;
		this.valuePos = valuePos;
		this.value = value;
	}
	
	public void exprId(int nextStep) {
		this.exprId = nextStep;
	}

	// TODO: I think this is getting ahead of what I actually want
	// I think I want to TDA this
	public int exprId() {
		return exprId;
	}

	@Override
	public String toString() {
		return "expr " + eval + " should have value ";
	}
}
