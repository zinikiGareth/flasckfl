package org.flasck.flas.vcode.hsieForm;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.rewrittenForm.ExternalRef;

public class PushExternal extends PushReturn {
	public final ExternalRef fn;

	public PushExternal(InputPosition loc, ExternalRef fn) {
		super(loc);
		this.fn = fn;
	}

	protected Object textValue() {
		return fn.getClass().getSimpleName()+"."+fn;
	}
}
