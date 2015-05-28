package org.flasck.flas.vcode.hsieForm;

// Push and Return are like REALLY, REALLY similar
// It helps the typechecker at least to treat them as exactly the same
public abstract class PushReturn extends HSIEBlock {
	public final Var var;
	public final Integer ival;
	public final String fn;

	public PushReturn(Var var) {
		this.var = var;
		this.ival = null;
		this.fn = null;
	}

	public PushReturn(int i) {
		this.var = null;
		this.ival = i;
		this.fn = null;
	}

	public PushReturn(String fn) {
		this.var = null;
		this.ival = null;
		this.fn = fn;
	}
}
