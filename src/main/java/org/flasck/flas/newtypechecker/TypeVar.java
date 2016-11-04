package org.flasck.flas.newtypechecker;

import org.flasck.flas.vcode.hsieForm.Var;

public class TypeVar extends TypeInfo {
	private final Var var;

	public TypeVar(Var var) {
		this.var = var;
	}
	
	@Override
	public String toString() {
		return var.toString();
	}
}
