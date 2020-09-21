package org.flasck.flas.parsedForm.st;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.commonBase.StringLiteral;

public class AjaxSubscribe {
	public final StringLiteral pathUrl;
	public final List<Expr> responses = new ArrayList<>();

	public AjaxSubscribe(InputPosition location, StringLiteral pathUrl) {
		this.pathUrl = pathUrl;
	}

	public void response(Expr expr) {
		responses.add(expr);
	}

}
