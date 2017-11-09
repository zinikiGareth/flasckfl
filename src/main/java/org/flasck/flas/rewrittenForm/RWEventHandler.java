package org.flasck.flas.rewrittenForm;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.SolidName;

public class RWEventHandler {
	public final String action;
	public final Object expr;
	public final SolidName handlerFn;
	private final InputPosition loc;

	public RWEventHandler(InputPosition loc, String action, Object expr, SolidName handler) {
		this.loc = loc;
		this.action = action;
		this.expr = expr;
		handlerFn = handler;
	}

	public InputPosition location() {
		return loc;
	}
}
