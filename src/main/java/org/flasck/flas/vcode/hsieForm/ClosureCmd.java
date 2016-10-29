package org.flasck.flas.vcode.hsieForm;

import org.flasck.flas.blockForm.InputPosition;

public class ClosureCmd extends HSIEBlock {
	public final Var var;
	public boolean justScoping = false;

	public ClosureCmd(InputPosition loc, Var var) {
		super(loc);
		this.var = var;
	}

	@Override
	public String toString() {
		return "CLOSURE " + var + (downcastType != null?" " + downcastType:"");
	}
}
