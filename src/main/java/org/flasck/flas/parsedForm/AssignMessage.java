package org.flasck.flas.parsedForm;

import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Expr;

public class AssignMessage implements ActionMessage {
	public final InputPosition kw;
	public final List<UnresolvedVar> slot;
	public final Expr expr;

	public AssignMessage(InputPosition kw, List<UnresolvedVar> slot, Expr expr) {
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
}
