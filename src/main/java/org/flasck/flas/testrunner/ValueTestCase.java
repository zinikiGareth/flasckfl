package org.flasck.flas.testrunner;

public class ValueTestCase implements TestCase {
	public final Object eval;

	public ValueTestCase(Object eval) {
		this.eval = eval;
	}

	@Override
	public String toString() {
		return "expr " + eval + " should have value ";
	}
}
