package org.flasck.flas.commonBase;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.parsedForm.UnresolvedOperator;
import org.zinutils.exceptions.UtilException;

public class ApplyExpr implements Expr {
	public final InputPosition location;
	public final Object fn;
	public final List<Object> args = new ArrayList<Object>();

	public ApplyExpr(InputPosition location, Object op, List<Object> args) {
		if (location == null)
			throw new UtilException("ApplyExpr without location");
		this.location = location;
		this.fn = op;
		this.args.addAll(args);
	}

	public ApplyExpr(InputPosition location, Object op, Object... args) {
		if (location == null)
			throw new UtilException("ApplyExpr without location");
		this.location = location;
		this.fn = op;
		for (Object o : args)
			this.args.add(o);
	}

	@Override
	public InputPosition location() {
		return location;
	}
	
	public void showTree(int ind) {
		showOne(ind, fn);
		for (Object o : args) {
			showOne(ind+2, o);
		}
	}

	private void showOne(int ind, Object o) {
		if (o instanceof ApplyExpr) {
			((ApplyExpr)o).showTree(ind);
		} else {
			for (int i=0;i<ind;i++)
				System.out.print(" ");
			System.out.println(o);
		}
	}
	
	@Override
	public String toString() {
		StringBuilder ret = new StringBuilder();
		ret.append("(");
		ret.append(fn);
		for (Object o : args) {
			ret.append(" ");
			ret.append(o);
		}
		ret.append(")");
		return ret.toString();
	}

	public static boolean isOp(Expr fe, String match) {
		if (!(fe instanceof ApplyExpr))
			return false;
		Object op = ((ApplyExpr)fe).fn;
		if (!(op instanceof UnresolvedOperator))
			return false;
		return ((UnresolvedOperator)op).op.equals(match);
	}
}
