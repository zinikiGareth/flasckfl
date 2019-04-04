package org.flasck.flas.parsedForm;

import java.util.List;

import org.flasck.flas.commonBase.names.FunctionName;

public class TupleAssignment {
	public final List<LocatedName> vars;
	private FunctionName leadName;
	public final Object expr;

	// We used located name here, not unresolvedvar, because this is defining the things
	public TupleAssignment(List<LocatedName> vars, FunctionName leadName, Object expr) {
		this.vars = vars;
		this.leadName = leadName;
		this.expr = expr;
	}

	public FunctionName leadName() {
		return leadName;
	}

}
