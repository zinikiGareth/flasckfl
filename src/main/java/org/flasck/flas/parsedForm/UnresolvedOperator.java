package org.flasck.flas.parsedForm;

import org.flasck.flas.blockForm.InputPosition;

public class UnresolvedOperator implements Locatable {
	public final InputPosition location;
	public final String op;

	public UnresolvedOperator(InputPosition location, String op) {
		this.location = location;
		this.op = op;
	}
	
	@Override
	public InputPosition location() {
		return location;
	}

	@Override
	public String toString() {
		return op;
	}
}
