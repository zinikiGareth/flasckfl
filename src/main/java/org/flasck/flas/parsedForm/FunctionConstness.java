package org.flasck.flas.parsedForm;

import java.util.List;

import org.flasck.flas.commonBase.names.NameOfThing;

public class FunctionConstness {
	public final String reason;
	public final List<NameOfThing> depends;

	public FunctionConstness(String reason) {
		this.reason = reason;
		this.depends = null;
	}

	public FunctionConstness(List<NameOfThing> depends) {
		this.reason = null;
		if (depends.isEmpty())
			this.depends = null;
		else
			this.depends = depends;
	}

	@Override
	public String toString() {
		if (reason != null)
			return reason;
		else if (depends != null)
			return depends.toString();
		else
			return "isConst";
	}

	public boolean isConstant() {
		return reason == null && depends == null;
	}
}
