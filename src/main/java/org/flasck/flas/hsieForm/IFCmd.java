package org.flasck.flas.hsieForm;

import org.flasck.flas.hsieForm.HSIEForm.Var;

public class IFCmd extends HSIEBlock {

	private final Var var;
	private final int value;

	// TODO: needs to handle more general cases (other values, arbitrary expressions)
	public IFCmd(Var var, int value) {
		this.var = var;
		this.value = value;
	}

	@Override
	public String toString() {
		return "IF " +var + " " + value;
	}
}
