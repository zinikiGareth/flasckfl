package org.flasck.flas.parsedForm;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Expr;

public class SendMessage implements ActionMessage {
	public final InputPosition kw;
	public final Expr expr;
	private Expr handler; // the handler after ->
	private Expr subscriberName; // the subscription after =>

	public SendMessage(InputPosition kw, Expr expr) {
		this.kw = kw;
		this.expr = expr;
	}
	
	public InputPosition location() {
		return kw;
	}
	
	public void handlerExpr(Expr t) {
		this.handler = t;
	}
	
	public Expr handlerExpr() {
		return this.handler;
	}
	
	public void subscriberNameExpr(Expr t) {
		this.subscriberName = t;
	}
	
	public Expr subscriberName() {
		return subscriberName;
	}
	
	@Override
	public String toString() {
		return "<- " + expr.toString() + " @ " + kw;
	}
}
