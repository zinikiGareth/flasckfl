package org.flasck.flas.parsedForm;

import java.io.Serializable;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Locatable;

@SuppressWarnings("serial")
public class UnresolvedOperator implements Locatable, Serializable {
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
