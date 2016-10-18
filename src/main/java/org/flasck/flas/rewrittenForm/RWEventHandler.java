package org.flasck.flas.rewrittenForm;

public class RWEventHandler {
	public final String action;
	public final Object expr;

	public RWEventHandler(String action, Object expr) {
		this.action = action;
		this.expr = expr;
	}
}
