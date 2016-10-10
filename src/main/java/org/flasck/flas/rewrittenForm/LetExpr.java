package org.flasck.flas.rewrittenForm;

public class LetExpr {
	public final String var;
	public final Object val;
	public final Object expr;

	public LetExpr(String var, Object val, Object expr) {
		this.var = var;
		this.val = val;
		this.expr = expr;
	}

}
