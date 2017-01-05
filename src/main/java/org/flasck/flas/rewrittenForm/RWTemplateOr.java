package org.flasck.flas.rewrittenForm;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Locatable;
import org.flasck.flas.commonBase.names.AreaName;
import org.flasck.flas.commonBase.names.FunctionName;

public class RWTemplateOr implements RWTemplateLine, Locatable {
	private final InputPosition location;
	public final Object cond;
	public final RWTemplateLine template;
	public final FunctionName fnName;

	public RWTemplateOr(InputPosition loc, Object expr, RWTemplateLine template, FunctionName fnName) {
		this.location = loc;
		this.cond = expr;
		this.template = template;
		this.fnName = fnName;
	}
	
	@Override
	public AreaName areaName() {
		return template.areaName();
	}

	@Override
	public InputPosition location() {
		return location;
	}
}
