package org.flasck.flas.commonBase.names;

import org.flasck.flas.commonBase.NameOfThing;
import org.zinutils.exceptions.UtilException;

public class AreaName implements NameOfThing {
	private final String simple;
	public final CardName cardName;

	public AreaName(CardName cardName, String areaName) {
		this.cardName = cardName;
		this.simple = areaName;
	}
	
	public String jsName() {
		return cardName.jsUName() + "." + simple;
	}

	public String javaName() {
		return cardName.javaName() + "." + simple;
	}
	
	@Override
	public String toString() {
		throw new UtilException("Yo!");
	}

	@Override
	public CardName containingCard() {
		return cardName;
	}
}
