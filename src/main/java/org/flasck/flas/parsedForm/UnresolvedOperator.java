package org.flasck.flas.parsedForm;

public class UnresolvedOperator {
	public final String op;

	public UnresolvedOperator(String op) {
		this.op = op;
	}

	@Override
	public String toString() {
		return op;
	}
}
