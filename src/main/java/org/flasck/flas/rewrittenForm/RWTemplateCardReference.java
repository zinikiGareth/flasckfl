package org.flasck.flas.rewrittenForm;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.template.TemplateLine;

public class RWTemplateCardReference implements TemplateLine {
	public final InputPosition location;
	public final Object explicitCard;
	public final Object yoyoVar;

	public RWTemplateCardReference(InputPosition location, Object cardVar, Object yoyoVar) {
		this.location = location;
		this.explicitCard = cardVar;
		this.yoyoVar = yoyoVar;
	}
}
