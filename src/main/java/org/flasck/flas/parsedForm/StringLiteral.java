package org.flasck.flas.parsedForm;

import org.flasck.flas.blockForm.InputPosition;

public class StringLiteral {
	public final String text;
	public final InputPosition location;

	public StringLiteral(InputPosition loc, String text) {
		this.location = loc;
		this.text = text;
	}
	
	@Override
	public String toString() {
		return '"' + text + '"';
	}

}
