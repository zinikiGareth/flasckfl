package org.flasck.flas.vcode.hsieForm;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.StringLiteral;

public class PushString extends PushReturn {
	public final StringLiteral sval;

	public PushString(InputPosition loc, StringLiteral s) {
		super(loc);
		this.sval = s;
	}

	protected Object textValue() {
		return sval;
	}
}
