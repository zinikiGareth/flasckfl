package org.flasck.flas.rewrittenForm;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.parsedForm.Locatable;

public class SpecialFormat implements Locatable {
	private final InputPosition location;
	public final String name;
	public final List<Object> args = new ArrayList<Object>();

	public SpecialFormat(InputPosition location, String name) {
		this.location = location;
		this.name = name;
	}

	@Override
	public InputPosition location() {
		return location;
	}
}
