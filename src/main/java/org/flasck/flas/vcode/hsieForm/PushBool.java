package org.flasck.flas.vcode.hsieForm;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.BooleanLiteral;

public class PushBool extends PushReturn {
	public final BooleanLiteral bval;

	public PushBool(InputPosition loc, BooleanLiteral bool) {
		super(loc);
		this.bval = bool;
	}

	protected Object textValue() {
		return Boolean.toString(bval.value());
	}
}
