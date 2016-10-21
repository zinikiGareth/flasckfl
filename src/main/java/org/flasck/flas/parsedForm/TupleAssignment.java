package org.flasck.flas.parsedForm;

import java.util.List;

public class TupleAssignment {
	public final List<LocatedName> vars;
	public final Object expr;

	public TupleAssignment(List<LocatedName> vars, Object expr) {
		this.vars = vars;
		this.expr = expr;
	}

}
