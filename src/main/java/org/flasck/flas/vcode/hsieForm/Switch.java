package org.flasck.flas.vcode.hsieForm;

import org.flasck.flas.blockForm.InputPosition;

public class Switch extends HSIEBlock {
	public final InputPosition location;
	public final String ctor;
	public final Var var;

	public Switch(InputPosition loc, Var var, String ctor) {
		location = loc;
		this.var = var;
		this.ctor = ctor;
	}

	@Override
	public String toString() {
		return "SWITCH " + var + " " + ctor;
	}
}
