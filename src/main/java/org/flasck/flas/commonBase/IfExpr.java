package org.flasck.flas.commonBase;

public class IfExpr {
	public final Object guard;
	public final Object ifExpr;
	public final Object elseExpr;

	public IfExpr(Object guard, Object ifExpr, Object elseExpr) {
		this.guard = guard;
		this.ifExpr = ifExpr;
		this.elseExpr = elseExpr;
	}
}
