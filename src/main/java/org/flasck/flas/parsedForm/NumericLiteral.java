package org.flasck.flas.parsedForm;

import org.flasck.flas.blockForm.InputPosition;

public class NumericLiteral {
	public final InputPosition location;
	public final String text;

	public NumericLiteral(InputPosition loc, String text) {
		this.location = loc;
		this.text = text;
	}

	@Override
	public String toString() {
		return text;
	}
}
