package org.flasck.flas.rewrittenForm;

import java.util.List;

import org.flasck.flas.commonBase.Locatable;

public class RWMethodMessage {
	public final List<Locatable> slot;
	public final Object expr;

	public RWMethodMessage(List<Locatable> slot, Object expr) {
		this.slot = slot;
		this.expr = expr;
	}
	
	@Override
	public String toString() {
		return (slot != null ? slot.toString() : "") + " <- " + expr.toString();
	}
}
