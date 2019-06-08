package org.flasck.flas.parsedForm;

import org.flasck.flas.commonBase.Expr;

public class TemplateBinding {
	public final String slot;
	public final Expr expr;
	public final String sendsTo;

	public TemplateBinding(String slot, Expr expr, String sendsTo) {
		this.slot = slot;
		this.expr = expr;
		this.sendsTo = sendsTo;
	}
	
	@Override
	public String toString() {
		return "Binding[" + slot + "<-"+ expr + "=>" + sendsTo+"]";
	}
}
