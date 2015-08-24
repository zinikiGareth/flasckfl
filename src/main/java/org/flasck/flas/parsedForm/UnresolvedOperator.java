package org.flasck.flas.parsedForm;

import org.flasck.flas.blockForm.InputPosition;
import org.zinutils.exceptions.UtilException;

public class UnresolvedOperator implements Locatable {
	public final InputPosition location;
	public final String op;

	public UnresolvedOperator(InputPosition location, String op) {
		if (location == null)
			throw new UtilException("not allowed");
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
