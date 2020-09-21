package org.flasck.flas.parsedForm.st;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Locatable;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.commonBase.names.VarName;

public class AjaxCreate implements Locatable {
	private final InputPosition loc;

	public AjaxCreate(InputPosition loc, VarName vn, StringLiteral baseUrl) {
		this.loc = loc;
	}

	public InputPosition location() {
		return loc;
	}

}
