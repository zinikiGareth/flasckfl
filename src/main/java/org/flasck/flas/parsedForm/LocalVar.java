package org.flasck.flas.parsedForm;

import java.io.Serializable;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.typechecker.Type;

@SuppressWarnings("serial")
public class LocalVar implements Locatable, Serializable {
	public final InputPosition varLoc;
	public final String definedBy;
	public final String var;
	public final InputPosition typeLoc;
	public final Type type;

	public LocalVar(String definedBy, InputPosition varLoc, String var, InputPosition typeLoc, Type type) {
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
