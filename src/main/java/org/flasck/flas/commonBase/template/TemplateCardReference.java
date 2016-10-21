package org.flasck.flas.commonBase.template;

import org.flasck.flas.blockForm.InputPosition;

public class TemplateCardReference implements TemplateLine {
	public final InputPosition location;
	public final Object explicitCard;
	public final Object yoyoVar;

	public TemplateCardReference(InputPosition location, Object cardName, Object yoyoName) {
		this.location = location;
		this.explicitCard = cardName;
		this.yoyoVar = yoyoName;
	}

}
