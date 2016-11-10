package org.flasck.flas.rewrittenForm;

import org.flasck.flas.blockForm.InputPosition;

public class RWTemplateCardReference implements RWTemplateLine {
	public final InputPosition location;
	public final Object explicitCard;
	public final Object yoyoVar;
	private final String areaName;
	public final String fnName;

	public RWTemplateCardReference(InputPosition location, Object cardVar, Object yoyoVar, String areaName, String fnName) {
		this.location = location;
		this.explicitCard = cardVar;
		this.yoyoVar = yoyoVar;
		this.areaName = areaName;
		this.fnName = fnName;
	}

	@Override
	public String areaName() {
		return areaName;
	}
}
