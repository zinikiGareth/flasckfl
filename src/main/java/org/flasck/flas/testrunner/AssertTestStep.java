package org.flasck.flas.testrunner;

import org.flasck.flas.blockForm.InputPosition;

public class AssertTestStep implements TestStep {
//	private final InputPosition evalPos;
	private final Object eval;
//	private final InputPosition valuePos;
	private final Object value;
	private final int exprId;

	public AssertTestStep(int exprId, InputPosition evalPos, Object eval, InputPosition valuePos, Object value) {
		this.exprId = exprId;
//		this.evalPos = evalPos;
		this.eval = eval;
//		this.valuePos = valuePos;
		this.value = value;
	}
	
	@Override
	public void run(TestRunner runner) throws Exception {
		runner.assertCorrectValue(exprId);
	}

	@Override
	public String toString() {
		return "expr " + eval + " should have value " + value;
	}
}
