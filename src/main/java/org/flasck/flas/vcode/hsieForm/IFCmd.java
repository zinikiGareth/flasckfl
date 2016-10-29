package org.flasck.flas.vcode.hsieForm;

import org.flasck.flas.blockForm.InputPosition;

public class IFCmd extends HSIEBlock {
	public final CreationOfVar var;
	public final Object value;

	// TODO: needs to handle more general cases (other values, arbitrary expressions)
	public IFCmd(InputPosition loc, CreationOfVar var, Object value) {
		super(loc);
		this.var = var;
		this.value = value;
	}

	public IFCmd(InputPosition loc, CreationOfVar var) {
		super(loc);
		this.var = var;
		this.value = null;
	}

	@Override
	public String toString() {
		return "IF " +var + (value!=null?" " + value:"");
	}
}
