package org.flasck.flas.testrunner;

public class AssertTestStep implements TestStep {
	public final Object eval;

	public AssertTestStep(Object eval) {
		this.eval = eval;
	}

	@Override
	public String toString() {
		return "expr " + eval + " should have value ";
	}
}
