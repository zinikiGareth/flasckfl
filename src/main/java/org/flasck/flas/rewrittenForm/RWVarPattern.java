package org.flasck.flas.rewrittenForm;

import java.io.Serializable;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Locatable;
import org.zinutils.exceptions.UtilException;

@SuppressWarnings("serial")
public class RWVarPattern implements Serializable, Locatable {
	public final InputPosition varLoc;
	public final String var;

	public RWVarPattern(InputPosition varLoc, String text) {
		this.varLoc = varLoc;
		if (!text.contains("."))
			throw new UtilException("yo");
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
