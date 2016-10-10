package org.flasck.flas.rewrittenForm;

import org.flasck.flas.blockForm.InputPosition;

public class RWTemplateListVar {
	public final InputPosition location;
	public final String name;

	public RWTemplateListVar(InputPosition location, String name) {
		this.location = location;
		this.name = name;
	}

	@Override
	public String toString() {
		return "TLV[" + name + "]";
	}
}
