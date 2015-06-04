package org.flasck.flas.vcode.hsieForm;

import org.flasck.flas.hsie.StringLiteral;

public class PushCmd extends PushReturn {

	public PushCmd(Var var) {
		super(var);
	}

	public PushCmd(int i) {
		super(i);
	}

	public PushCmd(String fn) {
		super(fn);
	}

	public PushCmd(StringLiteral s) {
		super(s);
	}

	@Override
	public String toString() {
		return "PUSH " + textValue();
	}
}
