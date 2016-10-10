package org.flasck.flas.rewrittenForm;

import java.io.Serializable;

import org.flasck.flas.blockForm.InputPosition;

@SuppressWarnings("serial")
public class StringLiteral implements Locatable, Serializable {
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