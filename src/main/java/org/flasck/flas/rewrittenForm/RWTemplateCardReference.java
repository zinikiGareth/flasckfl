package org.flasck.flas.rewrittenForm;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.AreaName;
import org.flasck.flas.commonBase.names.FunctionName;

public class RWTemplateCardReference implements RWTemplateLine {
	public final InputPosition location;
	public final Object explicitCard;
	public final Object yoyoVar;
	private final AreaName areaName;
	public final String fnName;

	public RWTemplateCardReference(InputPosition location, Object cardVar, Object yoyoVar, AreaName areaName, FunctionName fnName) {
		this.location = location;
		this.explicitCard = cardVar;
		this.yoyoVar = yoyoVar;
		this.areaName = areaName;
		this.fnName = fnName == null?null:fnName.jsName();
	}

	@Override
	public AreaName areaName() {
		return areaName;
	}
}
