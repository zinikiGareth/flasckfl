package org.flasck.flas.parsedForm;

import org.flasck.flas.commonBase.Expr;

public class TemplateBindingOption extends TemplateCustomization {
	public final TemplateField assignsTo;
	public final Expr cond;
	public final Expr expr;
	public final TemplateReference sendsTo;

	public TemplateBindingOption(TemplateField field, Expr cond, Expr expr, TemplateReference sendsTo) {
		this.assignsTo = field;
		this.cond = cond;
		this.expr = expr;
		this.sendsTo = sendsTo;
	}

	public TemplateBindingOption conditionalOn(Expr cond) {
		return new TemplateBindingOption(assignsTo, cond, expr, sendsTo);
	}
}
