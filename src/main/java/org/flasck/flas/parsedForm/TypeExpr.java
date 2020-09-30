package org.flasck.flas.parsedForm;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Expr;

public class TypeExpr implements Expr {
	public final InputPosition location;
	public final InputPosition tyLoc;
	public final Expr type;

	public TypeExpr(InputPosition location, InputPosition tyLoc, Expr ctor) {
		if (location == null)
			System.out.println("TypeExpr without location");
		this.location = location;
		this.tyLoc = tyLoc;
		this.type = ctor;
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
