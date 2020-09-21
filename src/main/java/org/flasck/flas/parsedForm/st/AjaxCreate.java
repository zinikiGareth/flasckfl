package org.flasck.flas.parsedForm.st;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Locatable;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.commonBase.names.VarName;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.ut.UnitTestStep;

public class AjaxCreate implements Locatable, UnitTestStep {
	private final InputPosition loc;
	public final VarName var;
	public final StringLiteral baseUrl;
	public final List<AjaxSubscribe> expectations = new ArrayList<>();

	public AjaxCreate(InputPosition loc, VarName vn, StringLiteral baseUrl) {
		this.loc = loc;
		this.var = vn;
		this.baseUrl = baseUrl;
	}

	public InputPosition location() {
		return loc;
	}

	public void subscribe(ErrorReporter errors, AjaxSubscribe sub) {
		expectations.add(sub);
	}
}
