package org.flasck.flas.parsedForm;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Expr;

public class CastExpr implements Expr {
	public final InputPosition location;
	public final InputPosition tyLoc;
	public final InputPosition valLoc;
	public final TypeReference type;
	public final Expr val;

	public CastExpr(InputPosition location, InputPosition tyLoc, InputPosition valLoc, TypeReference type, Expr val) {
		if (location == null)
			System.out.println("TypeExpr without location");
		this.location = location;
		this.tyLoc = tyLoc;
		this.valLoc = valLoc;
		this.type = type;
		this.val = val;
	}

	@Override
	public InputPosition location() {
		return location;
	}
	
	@Override
	public String toString() {
		return "(cast " + type + " " + val + ")";
	}
}
