package org.flasck.flas.parsedForm;

import java.util.ArrayList;
import java.util.List;

public class ApplyExpr {
	public final ItemExpr fn;
	public final List<Object> args = new ArrayList<Object>();

	public ApplyExpr(ItemExpr op, List<Object> args) {
		this.fn = op;
		this.args.addAll(args);
	}

	public ApplyExpr(ItemExpr op, Object... args) {
		this.fn = op;
		for (Object o : args)
			this.args.add(o);
	}

	public void showTree(int ind) {
		for (int i=0;i<ind;i++)
			System.out.print(" ");
		System.out.println(fn.tok.text);
		for (Object o : args) {
			if (o instanceof ItemExpr) {
				for (int i=0;i<ind+2;i++)
					System.out.print(" ");
				System.out.println(((ItemExpr)o).tok.text);
			}
			else
				((ApplyExpr)o).showTree(ind+2);
		}
	}
}
