package org.flasck.flas.hsieForm;

import org.flasck.flas.hsieForm.HSIEForm.Var;

public class PushCmd extends HSIEBlock {
	private final Var var;
	private final Integer ival;
	private final String fn;

	public PushCmd(Var var) {
		this.var = var;
		this.ival = null;
		this.fn = null;
	}

	public PushCmd(int i) {
		this.var = null;
		this.ival = i;
		this.fn = null;
	}

	public PushCmd(String fn) {
		this.var = null;
		this.ival = null;
		this.fn = fn;
	}

	@Override
	public String toString() {
		return "PUSH " + ((var != null)?var:(ival!=null)?ival.toString():(fn != null)?fn:"ERR");
	}
}
