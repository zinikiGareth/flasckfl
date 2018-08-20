package org.flasck.flas.rewrittenForm;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Locatable;

public class TypeCheckStringable implements Locatable {
	private final InputPosition location;
	public final Object expr;

	public TypeCheckStringable(InputPosition location, Object expr) {
		this.location = location;
		this.expr = expr;
	}

	@Override
	public InputPosition location() {
		return location;
	}

	@Override
	public String toString() {
		return "(#tcStringable " + expr + ")";
	}
}
