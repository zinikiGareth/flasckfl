package org.flasck.flas.parsedForm;

import org.flasck.flas.blockForm.InputPosition;

public class CardReference {
	public final InputPosition location;
	public final Object explicitCard;
	public final Object yoyoVar;

	public CardReference(InputPosition location, Object cardName, Object yoyoName) {
		this.location = location;
		this.explicitCard = cardName;
		this.yoyoVar = yoyoName;
	}

}
