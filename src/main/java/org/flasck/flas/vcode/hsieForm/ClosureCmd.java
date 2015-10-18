package org.flasck.flas.vcode.hsieForm;


public class ClosureCmd extends HSIEBlock {
	public final Var var;

	public ClosureCmd(Var var) {
		this.var = var;
	}

	@Override
	public String toString() {
		return "CLOSURE " + var + (downcastType != null?" " + downcastType:"");
	}
}
