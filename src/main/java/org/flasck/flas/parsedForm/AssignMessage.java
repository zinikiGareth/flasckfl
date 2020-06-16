package org.flasck.flas.parsedForm;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Expr;

public class AssignMessage implements ActionMessage {
	public final InputPosition kw;
	public final Expr slot;
	public final Expr expr;
	private boolean assignsToCons;

	public AssignMessage(InputPosition kw, Expr slot, Expr expr) {
		this.kw = kw;
		this.slot = slot;
		this.expr = expr;
	}
	
	@Override
	public InputPosition location() {
		return kw;
	}
	
	@Override
	public String toString() {
		return slot.toString() + " <- " + expr.toString();
	}

	public void willAssignToCons() {
		this.assignsToCons = true;
	}
	
	public boolean assignsToCons() {
		return assignsToCons;
	}
}
