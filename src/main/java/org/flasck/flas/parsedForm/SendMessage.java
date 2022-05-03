package org.flasck.flas.parsedForm;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Expr;
import org.zinutils.exceptions.NotImplementedException;

public class SendMessage implements ActionMessage {
	public final InputPosition kw;
	public final Expr expr;
	private Expr handlerName;

	public SendMessage(InputPosition kw, Expr expr) {
		this.kw = kw;
		this.expr = expr;
	}
	
	public InputPosition location() {
		return kw;
	}
	
	public void handlerNameExpr(Expr t) {
		this.handlerName = t;
	}
	
	public Expr handlerName() {
		return handlerName;
	}
	
	@Override
	public String toString() {
		return "<- " + expr.toString() + " @ " + kw;
	}
}
