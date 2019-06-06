package org.flasck.flas.parsedForm;

import org.flasck.flas.blockForm.InputPosition;

public class SendMessage implements ActionMessage {
	public final InputPosition kw;
	public final Object expr;

	public SendMessage(InputPosition kw, Object expr) {
		this.kw = kw;
		this.expr = expr;
	}
	
	public InputPosition location() {
		return kw;
	}
	
	@Override
	public String toString() {
		return "<- " + expr.toString() + " @ " + kw;
	}
}
