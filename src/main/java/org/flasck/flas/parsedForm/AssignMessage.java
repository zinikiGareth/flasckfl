package org.flasck.flas.parsedForm;

import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Locatable;

public class AssignMessage implements Locatable {
	public final InputPosition kw;
	public final List<UnresolvedVar> slot;
	public final Object expr;

	public AssignMessage(InputPosition kw, List<UnresolvedVar> slot, Object expr) {
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
