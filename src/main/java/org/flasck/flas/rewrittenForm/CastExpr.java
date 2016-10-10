package org.flasck.flas.rewrittenForm;

import java.io.Serializable;

import org.flasck.flas.blockForm.InputPosition;

@SuppressWarnings("serial")
public class CastExpr implements Locatable, Serializable {
	public final InputPosition location;
	public final Object castTo;
	public final Object expr;

	public CastExpr(InputPosition location, Object castTo, Object expr) {
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
