package org.flasck.flas.newtypechecker;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.vcode.hsieForm.Var;

public class TypeVar extends TypeInfo {
	private final InputPosition location;
	public final Var var;

	public TypeVar(InputPosition location, Var var) {
		this.location = location;
		this.var = var;
	}
	
	public InputPosition location() {
		return location;
	}
	
	@Override
	public String toString() {
		return var.toString();
	}
}
