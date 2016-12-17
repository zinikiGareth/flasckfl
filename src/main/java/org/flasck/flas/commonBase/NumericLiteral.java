package org.flasck.flas.commonBase;

import org.flasck.flas.blockForm.InputPosition;

public class NumericLiteral implements Expr {
	public final InputPosition location;
	public final String text;

	public NumericLiteral(InputPosition loc, String text, int end) {
		this.location = loc;
		this.location.endAt(end);
		this.text = text;
	}
	
	@Override
	public InputPosition location() {
		return location;
	}

	@Override
	public String toString() {
		return text;
	}
}
