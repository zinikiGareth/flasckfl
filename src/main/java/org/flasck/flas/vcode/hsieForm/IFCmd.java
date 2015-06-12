package org.flasck.flas.vcode.hsieForm;


public class IFCmd extends HSIEBlock {
	public final Var var;
	public final Object value;

	// TODO: needs to handle more general cases (other values, arbitrary expressions)
	public IFCmd(Var var, Object value) {
		this.var = var;
		this.value = value;
	}

	@Override
	public String toString() {
		return "IF " +var + " " + value;
	}
}
