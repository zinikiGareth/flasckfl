package org.flasck.flas.rewrittenForm;

import java.util.List;
import java.util.Set;

import org.flasck.flas.commonBase.Locatable;
import org.flasck.flas.rewriter.GatherScopedVars;

public class RWMethodMessage {
	public final List<Locatable> slot;
	public final Object expr;

	public RWMethodMessage(List<Locatable> slot, Object expr) {
		this.slot = slot;
		this.expr = expr;
	}
	
	public void gatherScopedVars(Set<ScopedVar> scopedVars) {
		new GatherScopedVars(scopedVars).dispatch(expr);
	}
	
	@Override
	public String toString() {
		return (slot != null ? slot.toString() : "") + " <- " + expr.toString();
	}
}
