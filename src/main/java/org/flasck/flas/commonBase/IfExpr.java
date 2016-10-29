package org.flasck.flas.commonBase;

import org.flasck.flas.blockForm.InputPosition;

public class IfExpr implements Locatable {
	public final Locatable guard;
	public final Object ifExpr;
	public final Object elseExpr;

	public IfExpr(Locatable guard, Object ifExpr, Object elseExpr) {
		this.guard = guard;
		this.ifExpr = ifExpr;
		this.elseExpr = elseExpr;
	}

	@Override
	public InputPosition location() {
		return guard.location();
	}
}
