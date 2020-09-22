package org.flasck.flas.parsedForm.st;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Locatable;
import org.flasck.flas.commonBase.names.VarName;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.ut.UnitTestStep;

public class AjaxPump implements Locatable, UnitTestStep {
	private final InputPosition loc;
	public final VarName var;
	public final List<AjaxSubscribe> expectations = new ArrayList<>();

	public AjaxPump(InputPosition loc, VarName vn) {
		this.loc = loc;
		this.var = vn;
	}

	public InputPosition location() {
		return loc;
	}

	public void subscribe(ErrorReporter errors, AjaxSubscribe sub) {
		expectations.add(sub);
	}
}
