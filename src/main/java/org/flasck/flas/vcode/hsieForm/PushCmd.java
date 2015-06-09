package org.flasck.flas.vcode.hsieForm;

import org.flasck.flas.parsedForm.ExternalRef;
import org.flasck.flas.parsedForm.StringLiteral;

public class PushCmd extends PushReturn {

	public PushCmd(Var var) {
		super(var);
	}

	public PushCmd(int i) {
		super(i);
	}

	public PushCmd(ExternalRef ref) {
		super(ref);
	}

	public PushCmd(StringLiteral s) {
		super(s);
	}

	@Override
	public String toString() {
		return "PUSH " + textValue();
	}
}
