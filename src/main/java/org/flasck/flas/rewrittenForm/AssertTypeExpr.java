package org.flasck.flas.rewrittenForm;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Locatable;
import org.flasck.flas.types.Type;

public class AssertTypeExpr implements Locatable {
	private InputPosition location;
	public final Object expr;
	public final Type type;

	public AssertTypeExpr(InputPosition location, Type slotType, Object expr) {
		this.location = location;
		this.type = slotType;
		this.expr = expr;
	}

	@Override
	public InputPosition location() {
		return location;
	}

	@Override
	public String toString() {
		return "(#assertType " + type + " " + expr + ")";
	}
}
