package org.flasck.flas.parsedForm;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.template.TemplateLine;

public class TemplateCardReference implements TemplateLine {
	public final InputPosition kw;
	public final InputPosition location;
	public final Object explicitCard;
	public final Object yoyoVar;

	public TemplateCardReference(InputPosition kw, InputPosition location, String cardName, String yoyoName) {
		this.kw = kw;
		this.location = location;
		this.explicitCard = cardName;
		this.yoyoVar = yoyoName;
	}

}
