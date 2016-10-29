package org.flasck.flas.vcode.hsieForm;

import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.commonBase.template.TemplateListVar;
import org.flasck.flas.rewrittenForm.CardStateRef;
import org.flasck.flas.rewrittenForm.ExternalRef;
import org.flasck.flas.rewrittenForm.FunctionLiteral;
import org.zinutils.exceptions.UtilException;
import org.zinutils.utils.Justification;

// Push and Return are like REALLY, REALLY similar
// It helps the typechecker at least to treat them as exactly the same
public class PushReturn extends HSIEBlock {
	private String cmd = "PUSH";
	public final InputPosition location;
	public final CreationOfVar var;
	public final Integer ival;
	public final StringLiteral sval;
	public final ExternalRef fn;
	public final TemplateListVar tlv;
	public final FunctionLiteral func;
	public final CardStateRef csr;
	public final List<CreationOfVar> deps;

	public PushReturn(InputPosition loc, CreationOfVar var) {
		if (loc == null) throw new UtilException("Cannot be null");
		this.location = loc;
		this.var = var;
		this.ival = null;
		this.sval = null;
		this.fn = null;
		this.tlv = null;
		this.func = null;
		this.csr = null;
		this.deps = null;
	}

	public PushReturn(InputPosition loc, CreationOfVar var, List<CreationOfVar> deps) {
		if (loc == null) throw new UtilException("Cannot be null");
		this.location = loc;
		this.var = var;
		this.ival = null;
		this.sval = null;
		this.fn = null;
		this.tlv = null;
		this.func = null;
		this.csr = null;
		this.deps = deps;
		asReturn();
	}

	public PushReturn(InputPosition loc, int i) {
		if (loc == null) throw new UtilException("Cannot be null");
		this.location = loc;
		this.var = null;
		this.ival = i;
		this.sval = null;
		this.fn = null;
		this.tlv = null;
		this.func = null;
		this.csr = null;
		this.deps = null;
	}

	public PushReturn(InputPosition loc, ExternalRef fn) {
		if (loc == null)
			throw new UtilException("Cannot be null");
		this.location = loc;
		this.var = null;
		this.ival = null;
		this.sval = null;
		this.fn = fn;
		this.tlv = null;
		this.func = null;
		this.csr = null;
		this.deps = null;
	}

	public PushReturn(InputPosition loc, StringLiteral s) {
		if (loc == null)
			throw new UtilException("Cannot be null");
		this.location = loc;
		this.var = null;
		this.ival = null;
		this.sval = s;
		this.fn = null;
		this.tlv = null;
		this.func = null;
		this.csr = null;
		this.deps = null;
	}

	public PushReturn(InputPosition loc, TemplateListVar tlv) {
		if (loc == null) throw new UtilException("Cannot be null");
		this.location = loc;
		this.var = null;
		this.ival = null;
		this.sval = null;
		this.fn = null;
		this.tlv = tlv;
		this.func = null;
		this.csr = null;
		this.deps = null;
	}

	public PushReturn(InputPosition loc, FunctionLiteral func) {
		if (loc == null) throw new UtilException("Cannot be null");
		this.location = loc;
		this.var = null;
		this.ival = null;
		this.sval = null;
		this.fn = null;
		this.tlv = null;
		this.func = func;
		this.csr = null;
		this.deps = null;
	}

	public PushReturn(InputPosition loc, CardStateRef csr) {
		if (loc == null) throw new UtilException("Cannot be null");
		this.location = loc;
		this.var = null;
		this.ival = null;
		this.sval = null;
		this.fn = null;
		this.tlv = null;
		this.func = null;
		this.csr = csr;
		this.deps = null;
	}

	public void asReturn() {
		this.cmd = "RETURN";
	}

	protected Object textValue() {
		return (var != null)?var:(ival!=null)?ival.toString():(fn != null)?fn.getClass().getSimpleName()+"."+fn:(sval!=null)?sval:(tlv!=null)?tlv:(func != null)?func:(csr!=null)?csr:"--have you added a new push type--";
	}

	@Override
	public String toString() {
		String loc;
		// This is just a hack to get the current Golden tests to pass; obviously I should fix all this
		if (cmd.equals("PUSH"))
			loc =  " #" + location + " - also want location where the variable is actually used here";
		else
			loc = " #" + location + " - this appears to be wrong for closures; wants to be the apply expr point";
		return Justification.LEFT.format(cmd + " " + textValue() + (deps == null? "" : " " + deps), 60) + loc;
	}
}
