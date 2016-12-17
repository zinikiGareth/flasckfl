package org.flasck.flas.parsedForm;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Expr;

public class CastExpr implements Expr {
	public final InputPosition location;
	public final InputPosition ctLoc;
	public final String castTo;
	public final Object expr;

	public CastExpr(InputPosition location, InputPosition ctLoc, String castTo, Object expr) {
		if (location == null)
			System.out.println("CastExpr without location");
		this.location = location;
		this.ctLoc = ctLoc;
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
