package org.flasck.flas.vcode.hsieForm;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.rewrittenForm.ExternalRef;
import org.flasck.flas.rewrittenForm.VarNestedFromOuterFunctionScope;

public class PushExternal extends PushReturn {
	public final ExternalRef fn;

	public PushExternal(InputPosition loc, ExternalRef fn) {
		super(loc);
		this.fn = fn;
	}

	protected Object textValue() {
		if (fn instanceof VarNestedFromOuterFunctionScope)
			return fn.toString();
		else
			return fn.getClass().getSimpleName()+"."+fn;
	}
}
