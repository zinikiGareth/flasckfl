package org.flasck.flas.parsedForm;

import java.util.List;

import org.flasck.flas.commonBase.names.FunctionName;

public class TupleAssignment {
	public final List<LocatedName> vars;
	private FunctionName exprFnName;
	public final Object expr;

	// We used located name here, not unresolvedvar, because this is defining the things
	public TupleAssignment(List<LocatedName> vars, FunctionName exprFnName, Object expr) {
		this.vars = vars;
		this.exprFnName = exprFnName;
		this.expr = expr;
	}

	public FunctionName exprFnName() {
		return exprFnName;
	}

}
