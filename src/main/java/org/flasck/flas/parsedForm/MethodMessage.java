package org.flasck.flas.parsedForm;

import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Locatable;

public class MethodMessage {
	public final InputPosition kw;
	public final List<Locatable> slot;
	public final Object expr;

	public MethodMessage(InputPosition kw, List<Locatable> slot, Object expr) {
		this.kw = kw;
		this.slot = slot;
		this.expr = expr;
	}
	
	@Override
	public String toString() {
		return (slot != null ? slot.toString() : "") + " <- " + expr.toString();
	}
}
