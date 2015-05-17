package org.flasck.flas.vcode.hsieForm;

import org.flasck.flas.vcode.hsieForm.HSIEForm.Var;

public class ReturnCmd extends HSIEBlock {
	private final Var var;
	private final Integer ival;
	private final String fn;

	public ReturnCmd(Var var) {
		this.var = var;
		this.ival = null;
		this.fn = null;
	}

	public ReturnCmd(int i) {
		this.var = null;
		this.ival = i;
		this.fn = null;
	}

	public ReturnCmd(String fn) {
		this.var = null;
		this.ival = null;
		this.fn = fn;
	}

	@Override
	public String toString() {
		return "RETURN " + ((var != null)?var:(ival!=null)?ival.toString():(fn != null)?fn:"ERR");
	}
}
