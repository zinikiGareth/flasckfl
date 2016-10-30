package org.flasck.flas.parsedForm;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Locatable;
import org.flasck.flas.commonBase.template.TemplateLine;

public class TemplateCardReference implements TemplateLine, Locatable {
	public final InputPosition kw;
	public final InputPosition location;
	public final String explicitCard;
	public final String yoyoVar;

	public TemplateCardReference(InputPosition kw, InputPosition location, String cardName, String yoyoName) {
		this.kw = kw;
		this.location = location;
		this.explicitCard = cardName;
		this.yoyoVar = yoyoName;
	}

	@Override
	public InputPosition location() {
		return location;
	}

}
