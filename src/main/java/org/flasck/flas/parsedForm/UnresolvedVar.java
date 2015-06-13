package org.flasck.flas.parsedForm;

import org.flasck.flas.blockForm.InputPosition;

public class UnresolvedVar {
	public final InputPosition location;
	public final String var;

	public UnresolvedVar(InputPosition location, String var) {
		this.location = location;
		this.var = var;
	}

	@Override
	public String toString() {
		return var;
	}
}
