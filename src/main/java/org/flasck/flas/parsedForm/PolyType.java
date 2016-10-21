package org.flasck.flas.parsedForm;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Locatable;

public class PolyType implements Locatable {
	private InputPosition location;
	private String name;

	public PolyType(InputPosition location, String name) {
		this.location = location;
		this.name = name;
	}

	public String name() {
		return name;
	}

	public InputPosition location() {
		return location;
	}

	@Override
	public String toString() {
		return name;
	}
}
