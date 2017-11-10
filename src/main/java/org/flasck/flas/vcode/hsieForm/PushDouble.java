package org.flasck.flas.vcode.hsieForm;

import org.flasck.flas.blockForm.InputPosition;

public class PushDouble extends PushReturn {
	public final Double dval;

	public PushDouble(InputPosition loc, double d) {
		super(loc);
		this.dval = d;
	}

	protected Object textValue() {
		return dval.toString();
	}
}
