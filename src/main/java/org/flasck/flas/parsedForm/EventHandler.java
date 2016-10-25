package org.flasck.flas.parsedForm;

import org.flasck.flas.blockForm.InputPosition;

public class EventHandler {
	public final InputPosition kw;
	public final InputPosition actionPos;
	public final String action;
	public final Object expr;

	public EventHandler(InputPosition kw, InputPosition ap, String action, Object expr) {
		this.kw = kw;
		this.actionPos = ap;
		this.action = action;
		this.expr = expr;
	}
}
