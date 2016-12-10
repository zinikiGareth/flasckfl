package org.flasck.flas.testrunner;

import org.flasck.flas.blockForm.InputPosition;

public class AssertTestStep implements TestStep {
	public final Object eval;
	public final InputPosition evalPos;

	public AssertTestStep(InputPosition evalPos, Object eval) {
		this.evalPos = evalPos;
		this.eval = eval;
	}

	@Override
	public String toString() {
		return "expr " + eval + " should have value ";
	}
}
