package org.flasck.flas.rewrittenForm;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.FunctionName;

public class RWEventHandler {
	public final String action;
	public final Object expr;
	public final FunctionName handlerFn;
	private final InputPosition loc;

	public RWEventHandler(InputPosition loc, String action, Object expr, FunctionName handler) {
		this.loc = loc;
		this.action = action;
		this.expr = expr;
		handlerFn = handler;
	}

	public InputPosition location() {
		return loc;
	}
}
