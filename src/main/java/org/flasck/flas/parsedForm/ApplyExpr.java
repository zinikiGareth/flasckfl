package org.flasck.flas.parsedForm;

import java.util.ArrayList;
import java.util.List;

public class ApplyExpr {
	public final Object fn;
	public final List<Object> args = new ArrayList<Object>();

	public ApplyExpr(Object op, List<Object> args) {
		this.fn = op;
		this.args.addAll(args);
	}

	public ApplyExpr(Object op, Object... args) {
		System.out.println("Applying " + op);
		this.fn = op;
		for (Object o : args)
			this.args.add(o);
	}

	public void showTree(int ind) {
		showOne(ind, fn);
		for (Object o : args) {
			showOne(ind+2, o);
		}
	}

	private void showOne(int ind, Object o) {
		if (o instanceof ItemExpr) {
			for (int i=0;i<ind;i++)
				System.out.print(" ");
			System.out.println(((ItemExpr)o).tok.text);
		}
		else
			((ApplyExpr)o).showTree(ind);
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
}
