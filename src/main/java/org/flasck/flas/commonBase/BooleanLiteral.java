package org.flasck.flas.commonBase;

import org.flasck.flas.blockForm.InputPosition;

public class BooleanLiteral implements Expr {
	private final InputPosition location;
	private final boolean value;

	public BooleanLiteral(InputPosition loc, boolean value) {
		this.location = loc;
		this.value = value;
	}

	@Override
	public InputPosition location() {
		return location;
	}

	public boolean value() {
		return value;
	}
	
	@Override
	public String toString() {
		return Boolean.toString(value);
	}
}
