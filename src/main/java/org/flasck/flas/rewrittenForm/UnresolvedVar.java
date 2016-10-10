package org.flasck.flas.rewrittenForm;

import java.io.Serializable;

import org.flasck.flas.blockForm.InputPosition;

@SuppressWarnings("serial")
public class UnresolvedVar implements Locatable, Serializable {
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
