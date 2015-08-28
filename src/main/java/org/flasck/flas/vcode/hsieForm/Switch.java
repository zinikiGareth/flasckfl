package org.flasck.flas.vcode.hsieForm;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.parsedForm.ExternalRef;

public class Switch extends HSIEBlock {
	public final InputPosition location;
	public final ExternalRef ctor;
	public final Var var;

	public Switch(InputPosition loc, Var var, ExternalRef ctor) {
		location = loc;
		this.var = var;
		this.ctor = ctor;
	}

	@Override
	public String toString() {
		return "SWITCH " + var + " " + ctor.uniqueName();
	}
}
