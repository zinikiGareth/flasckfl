package org.flasck.flas.rewrittenForm;

public class RWEventHandler {
	public final String action;
	public final Object expr;
	public final String handlerFn;

	public RWEventHandler(String action, Object expr, String handler) {
		this.action = action;
		this.expr = expr;
		handlerFn = handler;
	}
}
