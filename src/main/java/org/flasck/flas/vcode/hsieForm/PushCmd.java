package org.flasck.flas.vcode.hsieForm;


public class PushCmd extends HSIEBlock {
	public final Var var;
	public final Integer ival;
	public final String fn;

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
