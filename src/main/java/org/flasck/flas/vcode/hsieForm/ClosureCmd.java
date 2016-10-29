package org.flasck.flas.vcode.hsieForm;

import org.flasck.flas.blockForm.InputPosition;
import org.zinutils.utils.Justification;

public class ClosureCmd extends HSIEBlock {
	public final Var var;
	public boolean justScoping = false;

	public ClosureCmd(InputPosition loc, Var var) {
		super(loc);
		this.var = var;
	}

	@Override
	public String toString() {
		return Justification.LEFT.format("CLOSURE " + var + (downcastType != null?" " + downcastType:""), 60) + " ?? - should be beginning (and ending?) of apply expr";
	}
}
