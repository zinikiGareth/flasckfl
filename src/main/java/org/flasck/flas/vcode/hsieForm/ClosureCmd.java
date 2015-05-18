package org.flasck.flas.vcode.hsieForm;


public class ClosureCmd extends HSIEBlock {
	private final Var var;

	public ClosureCmd(Var var) {
		this.var = var;
	}

	@Override
	public String toString() {
		return "CLOSURE " + var;
	}
}
