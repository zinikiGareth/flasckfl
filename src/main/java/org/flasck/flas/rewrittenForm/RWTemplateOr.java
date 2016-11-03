package org.flasck.flas.rewrittenForm;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Locatable;
import org.flasck.flas.commonBase.template.TemplateLine;

public class RWTemplateOr implements TemplateLine, Locatable {
	private final InputPosition location;
	public final Object cond;
	public final TemplateLine template;
	public final String fnName;

	public RWTemplateOr(InputPosition loc, Object expr, TemplateLine template, String fnName) {
		this.location = loc;
		this.cond = expr;
		this.template = template;
		this.fnName = fnName;
	}

	@Override
	public InputPosition location() {
		return location;
	}
}
