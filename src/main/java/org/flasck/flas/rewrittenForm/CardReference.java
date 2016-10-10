package org.flasck.flas.rewrittenForm;

import java.io.Serializable;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.parsedForm.TemplateLine;

@SuppressWarnings("serial")
public class CardReference implements TemplateLine, Serializable {
	public final InputPosition location;
	public final Object explicitCard;
	public final Object yoyoVar;

	public CardReference(InputPosition location, Object cardName, Object yoyoName) {
		this.location = location;
		this.explicitCard = cardName;
		this.yoyoVar = yoyoName;
	}

}
