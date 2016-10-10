package org.flasck.flas.rewrittenForm;

public class FunctionClause {
	public final Object guard;
	public final Object expr;

	public FunctionClause(Object guard, Object expr) {
		this.guard = guard;
		this.expr = expr;
	}
}
