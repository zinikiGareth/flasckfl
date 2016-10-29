package org.flasck.flas.vcode.hsieForm;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.rewrittenForm.FunctionLiteral;

public class PushFunc extends PushReturn {
	public final FunctionLiteral func;

	public PushFunc(InputPosition loc, FunctionLiteral func) {
		super(loc);
		this.func = func;
	}
	protected Object textValue() {
		return func;
	}
}
