package org.flasck.flas.rewrittenForm;

import java.io.Serializable;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Locatable;
import org.flasck.flas.parsedForm.template.TemplateLine;

@SuppressWarnings("serial")
public class RWTemplateOr implements TemplateLine, Locatable, Serializable {
	private final InputPosition location;
	public final Object cond;
	public final TemplateLine template;

	public RWTemplateOr(InputPosition loc, Object expr, TemplateLine template) {
		this.location = loc;
		this.cond = expr;
		this.template = template;
	}

	@Override
	public InputPosition location() {
		return location;
	}
}
