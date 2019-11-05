package org.flasck.flas.commonBase;

import org.flasck.flas.blockForm.InputPosition;
import org.zinutils.exceptions.UtilException;

public class MemberExpr implements Expr {
	public final InputPosition location;
	public final Expr from;
	public final Expr fld;

	public MemberExpr(InputPosition location, Expr from, Expr fld) {
		if (location == null)
			throw new UtilException("MemberExpr without location");
		this.location = location;
		this.from = from;
		this.fld = fld;
	}

	@Override
	public InputPosition location() {
		return location;
	}
	
	public void showTree(int ind) {
		showOne(ind, from);
		showOne(ind, fld);
	}

	private void showOne(int ind, Object o) {
		for (int i=0;i<ind;i++)
			System.out.print(" ");
		System.out.println(o);
	}
	
	@Override
	public String toString() {
		StringBuilder ret = new StringBuilder();
		ret.append("(. ");
		ret.append(" ");
		ret.append(from);
		ret.append(" ");
		ret.append(fld);
		ret.append(")");
		return ret.toString();
	}
}
