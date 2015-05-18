package org.flasck.flas.hsie;

import java.util.List;

import org.flasck.flas.parsedForm.ConstPattern;
import org.flasck.flas.parsedForm.ConstructorMatch.Field;

public class NestedBinds {
	public final ConstPattern ifConst;
	public final List<Field> args;
	public final SubstExpr substExpr;

	public NestedBinds(ConstPattern patt, SubstExpr substExpr) {
		this.ifConst = patt;
		this.args = null;
		this.substExpr = substExpr;
	}

	public NestedBinds(List<Field> args, SubstExpr substExpr) {
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

	public void dump() {
		System.out.print("    ");
		if (ifConst != null)
			System.out.print(ifConst);
		else
			for (Field f : args)
				System.out.print(f.field + ": " + f.patt + " ");
		System.out.println("-> " + substExpr);
	}
}
