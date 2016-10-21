package org.flasck.flas.hsie;

import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.ConstPattern;
import org.flasck.flas.rewrittenForm.RWConstructorMatch.Field;

public class NestedBinds {
	public final InputPosition location;
	public final ConstPattern ifConst;
	public final List<Field> args;
	public final SubstExpr substExpr;

	public NestedBinds(InputPosition location, ConstPattern patt, SubstExpr substExpr) {
		this.location = location;
		this.ifConst = patt;
		this.args = null;
		this.substExpr = substExpr;
	}

	public NestedBinds(InputPosition location, List<Field> args, SubstExpr substExpr) {
		this.location = location;
		this.ifConst = null;
		this.args = args;
		this.substExpr = substExpr;
	}

	public Object matchField(String b) {
		for (Field f : args) {
			if (f.field.equals(b))
				return f.patt;
		}
		return null;
	}

	@Override
	public String toString() {
		return "NestedBinds[" + (ifConst != null?"const":"#args - " + args.size()) +"]";
	}
	
	public void dump() {
		System.out.print("    ");
		if (ifConst != null)
			System.out.print(ifConst);
		else
			for (Field f : args)
				System.out.print(f.field + ": " + f.patt + " ");
		System.out.println("-> " + substExpr);
	}

	public static InputPosition firstLocation(List<NestedBinds> list) {
		InputPosition ret = null;
		for (NestedBinds nb : list)
			if (ret == null)
				ret = nb.location;
			else if (ret.compareTo(nb.location) > 0)
				ret = nb.location;
		return ret;
	}
}
