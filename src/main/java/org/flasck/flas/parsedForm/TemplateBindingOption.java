package org.flasck.flas.parsedForm;

import java.util.Map;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.commonBase.Locatable;
import org.flasck.flas.tc3.NamedType;

public class TemplateBindingOption extends TemplateCustomization implements Locatable {
	public final TemplateField assignsTo;
	public final Expr cond;
	public final Expr expr;
	public final TemplateReference sendsTo;
	private Map<NamedType, Template> mapping;

	public TemplateBindingOption(TemplateField field, Expr cond, Expr expr, TemplateReference sendsTo) {
		this.assignsTo = field;
		this.cond = cond;
		this.expr = expr;
		this.sendsTo = sendsTo;
	}
	
	@Override
	public InputPosition location() {
		return assignsTo.location();
	}

	public TemplateBindingOption conditionalOn(Expr cond) {
		return new TemplateBindingOption(assignsTo, cond, expr, sendsTo);
	}
	
	public void attachMapping(Map<NamedType, Template> mapping) {
		this.mapping = mapping;
	}
	
	public Map<NamedType, Template> mapping() {
		return mapping;
	}
}
