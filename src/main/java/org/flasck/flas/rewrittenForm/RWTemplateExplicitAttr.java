package org.flasck.flas.rewrittenForm;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.FunctionName;

public class RWTemplateExplicitAttr {
	public final InputPosition location;
	public final String attr;
	public final int type;
	public final Object value;
	public final String fnName;

	public RWTemplateExplicitAttr(InputPosition loc, String attr, int type, Object value, FunctionName fnName) {
		this.location = loc;
		this.attr = attr;
		this.type = type;
		this.value = value;
		this.fnName = fnName.jsName();
	}
	
	@Override
	public String toString() {
		return "TEA[" + attr + ":" + type + "," + value + "]";
	}
}
