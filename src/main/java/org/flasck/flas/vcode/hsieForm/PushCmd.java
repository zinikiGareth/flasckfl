package org.flasck.flas.vcode.hsieForm;


public class PushCmd extends PushReturn {

	public PushCmd(Var var) {
		super(var);
	}

	public PushCmd(int i) {
		super(i);
	}

	public PushCmd(String fn) {
		super(fn);
	}

	@Override
	public String toString() {
		return "PUSH " + ((var != null)?var:(ival!=null)?ival.toString():(fn != null)?fn:"ERR");
	}
}
