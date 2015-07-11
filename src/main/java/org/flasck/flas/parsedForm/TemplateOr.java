package org.flasck.flas.parsedForm;

public class TemplateOr implements TemplateLine {
	public final Object cond;
	public final TemplateLine template;

	public TemplateOr(Object expr, TemplateLine template) {
		this.cond = expr;
		this.template = template;
	}
}
