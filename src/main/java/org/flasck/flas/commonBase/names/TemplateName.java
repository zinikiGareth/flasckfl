package org.flasck.flas.commonBase.names;

import org.flasck.flas.commonBase.NameOfThing;

public class TemplateName implements NameOfThing {
	private final CardName cardName;
	private String name;

	public TemplateName(CardName cardName) {
		this.cardName = cardName;
		this.name = null;
	}
	
	public TemplateName(CardName cardName, String name) {
		this.cardName = cardName;
		this.name = name;
	}

	public String baseName() {
		return name;
	}
	
	@Override
	public String jsName() {
		String cn = cardName.jsName();
		if (name != null)
			return cn + "." + name;
		return cn;
	}

	@Override
	public CardName containingCard() {
		return cardName;
	}

}
