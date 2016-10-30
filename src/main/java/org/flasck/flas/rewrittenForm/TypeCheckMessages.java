package org.flasck.flas.rewrittenForm;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.ApplyExpr;
import org.flasck.flas.commonBase.Locatable;

public class TypeCheckMessages implements Locatable {
	private final InputPosition location;
	public final ApplyExpr expr;

	public TypeCheckMessages(InputPosition location, ApplyExpr expr) {
		this.location = location;
		this.expr = expr;
	}

	@Override
	public InputPosition location() {
		return location;
	}

	@Override
	public String toString() {
		return "(#tcMessages " + expr + ")";
	}
}
