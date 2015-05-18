package org.flasck.flas.vcode.hsieForm;


public class Switch extends HSIEBlock {
	private final Var var;
	private final String ctor;

	public Switch(Var var, String ctor) {
		this.var = var;
		this.ctor = ctor;
	}

	@Override
	public String toString() {
		return "SWITCH " + var + " " + ctor;
	}
}
