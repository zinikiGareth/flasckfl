package org.flasck.flas.commonBase.template;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Locatable;

public class TemplateListVar implements Locatable {
	public final InputPosition location;
	public final String name;

	public TemplateListVar(InputPosition location, String name) {
		this.location = location;
		this.name = name;
	}
	
	@Override
	public InputPosition location() {
		return location;
	}

	@Override
	public String toString() {
		return "TLV[" + name + "]";
	}
}
