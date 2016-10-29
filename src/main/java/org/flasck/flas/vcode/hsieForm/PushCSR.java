package org.flasck.flas.vcode.hsieForm;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.rewrittenForm.CardStateRef;

public class PushCSR extends PushReturn {
	public final CardStateRef csr;

	public PushCSR(InputPosition loc, CardStateRef csr) {
		super(loc);
		this.csr = csr;
	}

	protected Object textValue() {
		return csr;
	}
}
