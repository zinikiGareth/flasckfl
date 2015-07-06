package org.flasck.flas.vcode.hsieForm;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.parsedForm.ExternalRef;
import org.flasck.flas.parsedForm.FunctionLiteral;
import org.flasck.flas.parsedForm.StringLiteral;
import org.flasck.flas.parsedForm.TemplateListVar;

// Push and Return are like REALLY, REALLY similar
// It helps the typechecker at least to treat them as exactly the same
public abstract class PushReturn extends HSIEBlock {
	public final InputPosition location;
	public final Var var;
	public final Integer ival;
	public final StringLiteral sval;
	public final ExternalRef fn;
	public final TemplateListVar tlv;
	public final FunctionLiteral func;

	public PushReturn(InputPosition loc, Var var) {
		this.location = loc;
		this.var = var;
		this.ival = null;
		this.sval = null;
		this.fn = null;
		this.tlv = null;
		this.func = null;
	}

	public PushReturn(InputPosition loc, int i) {
		this.location = loc;
		this.var = null;
		this.ival = i;
		this.sval = null;
		this.fn = null;
		this.tlv = null;
		this.func = null;
	}

	public PushReturn(InputPosition loc, ExternalRef fn) {
		this.location = loc;
		this.var = null;
		this.ival = null;
		this.sval = null;
		this.fn = fn;
		this.tlv = null;
		this.func = null;
	}

	public PushReturn(InputPosition loc, StringLiteral s) {
		this.location = loc;
		this.var = null;
		this.ival = null;
		this.sval = s;
		this.fn = null;
		this.tlv = null;
		this.func = null;
	}

	public PushReturn(InputPosition loc, TemplateListVar tlv) {
		this.location = loc;
		this.var = null;
		this.ival = null;
		this.sval = null;
		this.fn = null;
		this.tlv = tlv;
		this.func = null;
	}

	public PushReturn(InputPosition loc, FunctionLiteral func) {
		this.location = loc;
		this.var = null;
		this.ival = null;
		this.sval = null;
		this.fn = null;
		this.tlv = null;
		this.func = func;
	}

	protected Object textValue() {
		return (var != null)?var:(ival!=null)?ival.toString():(fn != null)?fn:(sval!=null?sval:"ERR");
	}
}
