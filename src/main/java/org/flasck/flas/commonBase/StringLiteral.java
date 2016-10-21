package org.flasck.flas.commonBase;

import org.flasck.flas.blockForm.InputPosition;

public class StringLiteral implements Locatable {
	public final String text;
	public final InputPosition location;

	public StringLiteral(InputPosition loc, String text) {
		this.location = loc;
		this.text = text;
	}
	
	@Override
	public InputPosition location() {
		return location;
	}
	
	@Override
	public String toString() {
		return '"' + text + '"';
	}
}