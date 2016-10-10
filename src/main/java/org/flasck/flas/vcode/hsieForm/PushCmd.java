package org.flasck.flas.vcode.hsieForm;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.parsedForm.CardStateRef;
import org.flasck.flas.parsedForm.FunctionLiteral;
import org.flasck.flas.parsedForm.StringLiteral;
import org.flasck.flas.parsedForm.TemplateListVar;
import org.flasck.flas.rewrittenForm.ExternalRef;
import org.zinutils.utils.Justification;

public class PushCmd extends PushReturn {

	public PushCmd(InputPosition loc, CreationOfVar var) {
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

	public PushCmd(InputPosition loc, CardStateRef csr) {
		super(loc, csr);
	}

	@Override
	public String toString() {
		return Justification.LEFT.format("PUSH " + textValue(), 60) + " " + location + " - also want location where the variable is actually used here";
	}
}
