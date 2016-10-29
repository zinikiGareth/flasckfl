package org.flasck.flas.vcode.hsieForm;

import org.flasck.flas.blockForm.InputPosition;
import org.zinutils.utils.Justification;

public class Switch extends HSIEBlock {
	public final String ctor;
	public final Var var;

	public Switch(InputPosition loc, Var var, String ctor) {
		super(loc);
		this.var = var;
		this.ctor = ctor;
	}

	@Override
	public String toString() {
		return Justification.LEFT.format("SWITCH " + var + " " + ctor, 60) + " #" + location;
	}
}
