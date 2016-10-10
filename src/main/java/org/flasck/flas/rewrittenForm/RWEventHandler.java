package org.flasck.flas.rewrittenForm;

import java.io.Serializable;

@SuppressWarnings("serial")
public class RWEventHandler implements Serializable {
	public final String action;
	public final Object expr;

	public RWEventHandler(String action, Object expr) {
		this.action = action;
		this.expr = expr;
	}
}
