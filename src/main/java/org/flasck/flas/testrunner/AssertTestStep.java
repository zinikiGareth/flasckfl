package org.flasck.flas.testrunner;

import org.flasck.flas.blockForm.InputPosition;

public class AssertTestStep implements TestStep {
	public final InputPosition evalPos;
	public final Object eval;
	public final InputPosition valuePos;
	public final Object value;

	public AssertTestStep(InputPosition evalPos, Object eval, InputPosition valuePos, Object value) {
		this.evalPos = evalPos;
		this.eval = eval;
		this.valuePos = valuePos;
		this.value = value;
	}

	@Override
	public String toString() {
		return "expr " + eval + " should have value ";
	}
}
