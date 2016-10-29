package org.flasck.flas.vcode.hsieForm;

import org.flasck.flas.blockForm.InputPosition;

public class PushInt extends PushReturn {
	public final Integer ival;

	public PushInt(InputPosition loc, int i) {
		super(loc);
		this.ival = i;
	}

	protected Object textValue() {
		return ival.toString();
	}
}
