package org.flasck.flas.vcode.hsieForm;

import org.flasck.flas.parsedForm.StringLiteral;

// Push and Return are like REALLY, REALLY similar
// It helps the typechecker at least to treat them as exactly the same
public abstract class PushReturn extends HSIEBlock {
	public final Var var;
	public final Integer ival;
	public final StringLiteral sval;
	public final String fn;

	public PushReturn(Var var) {
		this.var = var;
		this.ival = null;
		this.sval = null;
		this.fn = null;
	}

	public PushReturn(int i) {
		this.var = null;
		this.ival = i;
		this.sval = null;
		this.fn = null;
	}

	public PushReturn(String fn) {
		this.var = null;
		this.ival = null;
		this.sval = null;
		this.fn = fn;
	}

	public PushReturn(StringLiteral s) {
		this.var = null;
		this.ival = null;
		this.sval = s;
		this.fn = null;
	}

	protected Object textValue() {
		return (var != null)?var:(ival!=null)?ival.toString():(fn != null)?fn:(sval!=null?sval:"ERR");
	}
}
