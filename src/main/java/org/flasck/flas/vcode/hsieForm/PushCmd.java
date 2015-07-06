package org.flasck.flas.vcode.hsieForm;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.parsedForm.ExternalRef;
import org.flasck.flas.parsedForm.FunctionLiteral;
import org.flasck.flas.parsedForm.StringLiteral;
import org.flasck.flas.parsedForm.TemplateListVar;

public class PushCmd extends PushReturn {

	public PushCmd(InputPosition loc, Var var) {
		super(loc, var);
	}

	public PushCmd(InputPosition loc, int i) {
		super(loc, i);
	}

	public PushCmd(InputPosition loc, ExternalRef ref) {
		super(loc, ref);
	}

	public PushCmd(InputPosition loc, StringLiteral s) {
		super(loc, s);
	}

	public PushCmd(InputPosition loc, TemplateListVar tlv) {
		super(loc, tlv);
	}

	public PushCmd(InputPosition loc, FunctionLiteral func) {
		super(loc, func);
	}

	@Override
	public String toString() {
		return "PUSH " + textValue();
	}
}
