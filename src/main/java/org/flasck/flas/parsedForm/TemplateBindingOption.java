package org.flasck.flas.parsedForm;

import java.util.Map;

import org.flasck.flas.commonBase.Expr;

public class TemplateBindingOption extends TemplateCustomization {
	public final TemplateField assignsTo;
	public final Expr cond;
	public final Expr expr;
	public final TemplateReference sendsTo;
	private Map<StructDefn, Template> mapping;

	public TemplateBindingOption(TemplateField field, Expr cond, Expr expr, TemplateReference sendsTo) {
		this.assignsTo = field;
		this.cond = cond;
		this.expr = expr;
		this.sendsTo = sendsTo;
	}

	public TemplateBindingOption conditionalOn(Expr cond) {
		return new TemplateBindingOption(assignsTo, cond, expr, sendsTo);
	}
	
	public void attachMapping(Map<StructDefn, Template> mapping) {
		this.mapping = mapping;
	}
	
	public Map<StructDefn, Template> mapping() {
		return mapping;
	}
}
