package org.flasck.flas.parsedForm;

import org.flasck.flas.blockForm.InputPosition;

public class LocatedName {
	public final InputPosition location;
	public final String text;

	public LocatedName(InputPosition location, String text) {
		this.location = location;
		this.text = text;
	}

	@Override
	public String toString() {
		return text;
	}
}
