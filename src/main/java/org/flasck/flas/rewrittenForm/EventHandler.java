package org.flasck.flas.rewrittenForm;

import java.io.Serializable;

@SuppressWarnings("serial")
public class EventHandler implements Serializable {
	public final String action;
	public final Object expr;

	public EventHandler(String action, Object expr) {
		this.action = action;
		this.expr = expr;
	}
}
