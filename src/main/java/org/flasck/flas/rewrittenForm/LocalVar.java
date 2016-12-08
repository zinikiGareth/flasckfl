package org.flasck.flas.rewrittenForm;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Locatable;
import org.flasck.flas.types.Type;

public class LocalVar implements Locatable {
	public final InputPosition varLoc;
	public final String fnName;
	public final String caseName;
	public final String var;
	public final InputPosition typeLoc;
	public final Type type;

	public LocalVar(String fnName, String caseName, InputPosition varLoc, String var, InputPosition typeLoc, Type type) {
		this.fnName = fnName;
		this.varLoc = varLoc;
		this.caseName = caseName;
		this.var = var;
		this.typeLoc = typeLoc;
		this.type = type;
	}
	
	public String uniqueName() {
		return caseName + "." + var;
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
