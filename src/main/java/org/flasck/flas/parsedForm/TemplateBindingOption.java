package org.flasck.flas.parsedForm;

import java.util.Map;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.commonBase.Locatable;
import org.flasck.flas.tc3.NamedType;

public class TemplateBindingOption extends TemplateCustomization implements Locatable {
	private final InputPosition location;
	public final TemplateField assignsTo;
	public final Expr cond;
	public final Expr expr;
	public final TemplateReference sendsTo;
	private Map<NamedType, Template> mapping;

	public TemplateBindingOption(InputPosition loc, TemplateField field, Expr cond, Expr expr, TemplateReference sendsTo) {
		this.location = loc;
		this.assignsTo = field;
		this.cond = cond;
		this.expr = expr;
		this.sendsTo = sendsTo;
	}
	
	@Override
	public InputPosition location() {
		return location;
	}

	public TemplateBindingOption conditionalOn(InputPosition barPos, Expr cond) {
		return new TemplateBindingOption(barPos, assignsTo, cond, expr, sendsTo);
	}
	
	public void attachMapping(Map<NamedType, Template> mapping) {
		this.mapping = mapping;
	}
	
	public Map<NamedType, Template> mapping() {
		return mapping;
	}
}
