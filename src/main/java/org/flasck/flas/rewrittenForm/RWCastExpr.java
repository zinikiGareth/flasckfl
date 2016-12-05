package org.flasck.flas.rewrittenForm;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Locatable;

public class RWCastExpr implements Locatable {
	public final InputPosition location;
	public final Object castTo;
	public final Object expr;

	public RWCastExpr(InputPosition location, Object castTo, Object expr) {
		if (location == null)
			System.out.println("CastExpr without location");
		this.location = location;
		this.castTo = castTo;
		this.expr = expr;
	}

	@Override
	public InputPosition location() {
		return location;
	}
	
	@Override
	public String toString() {
		return "(downcast " + castTo + " " + expr.toString() + ")";
	}
}
