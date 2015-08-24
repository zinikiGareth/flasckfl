package org.flasck.flas.parsedForm;

import org.flasck.flas.blockForm.InputPosition;

public class TemplateOr implements TemplateLine, Locatable {
	private final InputPosition location;
	public final Object cond;
	public final TemplateLine template;

	public TemplateOr(InputPosition loc, Object expr, TemplateLine template) {
		this.location = loc;
		this.cond = expr;
		this.template = template;
	}

	@Override
	public InputPosition location() {
		return location;
	}
}
