package org.flasck.flas.parsedForm.st;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Locatable;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.commonBase.names.VarName;
import org.flasck.flas.parsedForm.ut.UnitTestStep;

public class AjaxCreate implements Locatable, UnitTestStep {
	private final InputPosition loc;
	public final VarName var;
	public final StringLiteral baseUrl;

	public AjaxCreate(InputPosition loc, VarName vn, StringLiteral baseUrl) {
		this.loc = loc;
		this.var = vn;
		this.baseUrl = baseUrl;
	}

	public InputPosition location() {
		return loc;
	}

}
