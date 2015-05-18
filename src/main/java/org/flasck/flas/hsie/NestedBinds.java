package org.flasck.flas.hsie;

import java.util.List;

import org.flasck.flas.parsedForm.ConstructorMatch.Field;

public class NestedBinds {
	public final List<Field> args;
	public final SubstExpr substExpr;

	public NestedBinds(List<Field> args, SubstExpr substExpr) {
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
		for (Field f : args)
			System.out.print(f.field + ": " + f.patt + " ");
		System.out.println("-> " + substExpr);
	}
}
