package org.flasck.flas.parsedForm;

import java.util.ArrayList;
import java.util.List;

public class TupleAssignment {
	public final List<LocatedName> vars;
	public final Object expr;

	@Deprecated // I want to go to UnresolvedVar, but that's not what the downstream expects at the moment
	public TupleAssignment(boolean deprecated, List<LocatedName> vars, Object expr) {
		this.vars = vars;
		this.expr = expr;
	}

	public TupleAssignment(List<UnresolvedVar> vars, Object expr) {
		this.vars = new ArrayList<>();
		for (UnresolvedVar v : vars) {
			this.vars.add(new LocatedName(v.location(), v.var));
		}
		this.expr = expr;
	}

}
