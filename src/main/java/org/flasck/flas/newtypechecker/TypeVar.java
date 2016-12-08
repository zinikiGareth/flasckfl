package org.flasck.flas.newtypechecker;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.vcode.hsieForm.Var;
import org.zinutils.exceptions.UtilException;

public class TypeVar extends TypeInfo {
	private final InputPosition location;
	public final Var var;

	public TypeVar(InputPosition location, Var var) {
		if (location == null)
			throw new UtilException("TypeVar without input location");
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
