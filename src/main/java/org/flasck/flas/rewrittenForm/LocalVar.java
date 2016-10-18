package org.flasck.flas.rewrittenForm;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Locatable;
import org.flasck.flas.typechecker.Type;
import org.zinutils.exceptions.UtilException;

public class LocalVar implements Locatable {
	public final InputPosition varLoc;
	public final String definedBy;
	public final String var;
	public final InputPosition typeLoc;
	public final Type type;

	public LocalVar(String definedBy, InputPosition varLoc, String var, InputPosition typeLoc, Type type) {
		if (definedBy.endsWith("loadCroset"))
			throw new UtilException("yo");
		this.varLoc = varLoc;
		this.definedBy = definedBy;
		this.var = var;
		this.typeLoc = typeLoc;
		this.type = type;
	}
	
	public String uniqueName() {
		return definedBy + "." + var;
	}
	
	@Override
	public InputPosition location() {
		return varLoc;
	}
	
	@Override
	public String toString() {
		return uniqueName();
	}
}
