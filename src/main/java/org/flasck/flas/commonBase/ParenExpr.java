package org.flasck.flas.commonBase;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.zinutils.exceptions.UtilException;

public class ParenExpr implements Expr {
	public final InputPosition location;
	public final Object expr;
	public final List<Object> args = new ArrayList<Object>();

	public ParenExpr(InputPosition location, Object expr) {
		if (location == null)
			throw new UtilException("ParenExpr without location");
		this.location = location;
		this.expr = expr;
	}

	@Override
	public InputPosition location() {
		return location;
	}
	
	@Override
	public String toString() {
		StringBuilder ret = new StringBuilder();
		ret.append("(");
		ret.append(expr);
		ret.append(")");
		return ret.toString();
	}
}
