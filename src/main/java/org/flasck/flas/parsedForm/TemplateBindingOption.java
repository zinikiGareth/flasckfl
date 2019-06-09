package org.flasck.flas.parsedForm;

import org.flasck.flas.commonBase.Expr;

public class TemplateBindingOption extends TemplateCustomization {
	public final Expr cond;
	public final Expr expr;
	public final String sendsTo;

	public TemplateBindingOption(Expr cond, Expr expr, String sendsTo) {
		this.cond = cond;
		this.expr = expr;
		this.sendsTo = sendsTo;
	}

	public TemplateBindingOption conditionalOn(Expr cond) {
		return new TemplateBindingOption(cond, expr, sendsTo);
	}
}
