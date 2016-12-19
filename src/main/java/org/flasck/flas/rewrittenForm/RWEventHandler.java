package org.flasck.flas.rewrittenForm;

import org.flasck.flas.commonBase.names.FunctionName;

public class RWEventHandler {
	public final String action;
	public final Object expr;
	public final FunctionName handlerFn;

	public RWEventHandler(String action, Object expr, FunctionName handler) {
		this.action = action;
		this.expr = expr;
		handlerFn = handler;
	}
}
