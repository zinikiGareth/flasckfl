package org.flasck.flas.parsedForm;

public class EventHandler {
	public final String action;
	public final Object expr;

	public EventHandler(String action, Object expr) {
		this.action = action;
		this.expr = expr;
	}
}
