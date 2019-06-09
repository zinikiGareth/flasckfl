package org.flasck.flas.parsedForm;

import org.flasck.flas.commonBase.Expr;

public class TemplateEvent {
	public final String event;
	public final Expr expr;

	public TemplateEvent(String event, Expr cond) {
		this.event = event;
		this.expr = cond;
	}
}
