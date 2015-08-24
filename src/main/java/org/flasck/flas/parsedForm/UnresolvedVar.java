package org.flasck.flas.parsedForm;

import org.flasck.flas.blockForm.InputPosition;

public class UnresolvedVar implements Locatable {
	public final InputPosition location;
	public final String var;

	public UnresolvedVar(InputPosition location, String var) {
		this.location = location;
		this.var = var;
	}

	@Override
	public InputPosition location() {
		return location;
	}

	@Override
	public String toString() {
		return var;
	}
}
