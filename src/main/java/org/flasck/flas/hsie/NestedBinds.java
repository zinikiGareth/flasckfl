package org.flasck.flas.hsie;

import java.util.List;

import org.flasck.flas.parsedForm.ConstructorMatch.Field;

public class NestedBinds {
	private final List<Field> args;
	private final SubstExpr substExpr;

	public NestedBinds(List<Field> args, SubstExpr substExpr) {
		this.args = args;
		this.substExpr = substExpr;
	}

	public void dump() {
		System.out.print("    ");
		for (Field f : args)
			System.out.print(f.field + ": " + f.patt + " ");
		System.out.println("-> " + substExpr);
	}
}
