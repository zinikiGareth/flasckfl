package org.flasck.flas.commonBase;

import java.io.Serializable;

import org.flasck.flas.blockForm.InputPosition;

@SuppressWarnings("serial")
public class NumericLiteral implements Locatable, Serializable {
	public final InputPosition location;
	public final String text;

	public NumericLiteral(InputPosition loc, String text) {
		this.location = loc;
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
