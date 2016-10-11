package org.flasck.flas.parsedForm;

import java.io.Serializable;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Locatable;

@SuppressWarnings("serial")
public class VarPattern implements Serializable, Locatable {
	public final InputPosition varLoc;
	public final String var;

	public VarPattern(InputPosition varLoc, String text) {
		this.varLoc = varLoc;
		this.var = text;
	}
	
	@Override
	public String toString() {
		return var;
	}

	@Override
	public InputPosition location() {
		return varLoc;
	}
}
