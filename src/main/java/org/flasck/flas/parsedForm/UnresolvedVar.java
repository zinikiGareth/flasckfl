package org.flasck.flas.parsedForm;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Expr;

public class UnresolvedVar implements Expr {
	public final InputPosition location;
	public final String var;

	public UnresolvedVar(InputPosition location, String var) {
		this.location = location;
		this.var = var;
	}

	@Override
	public InputPosition location() {
		return location;
	}

	@Override
	public String toString() {
		return var;
	}

	public boolean isConstructor() {
		return Character.isUpperCase(var.charAt(0));
	}

	public boolean isType() {
		return var.equals("type");
	}
}
