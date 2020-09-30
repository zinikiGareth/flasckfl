package org.flasck.flas.parsedForm;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Expr;

public class CheckTypeExpr implements Expr {
	public final InputPosition location;
	public final InputPosition tyLoc;
	public final TypeReference type;
	public final Expr expr;

	public CheckTypeExpr(InputPosition location, InputPosition tyLoc, Expr ctor, Expr expr) {
		if (location == null)
			System.out.println("CheckTypeExpr without location");
		this.location = location;
		this.tyLoc = tyLoc;
		this.type = (TypeReference) ctor;
		this.expr = expr;
	}

	@Override
	public InputPosition location() {
		return location;
	}
	
	@Override
	public String toString() {
		return "(type " + type + ")";
	}
}
