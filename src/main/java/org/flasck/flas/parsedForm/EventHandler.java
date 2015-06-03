package org.flasck.flas.parsedForm;

public class EventHandler {
	public final String text;
	public final String var;
	public final Object expr;

	public EventHandler(String text, String var, Object expr) {
		this.text = text;
		this.var = var;
		this.expr = expr;
	}
}
