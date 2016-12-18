package org.flasck.flas.commonBase.names;

import org.flasck.flas.commonBase.NameOfThing;

public class TemplateName implements NameOfThing, Comparable<TemplateName> {
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
	
	public String uniqueName() {
		if (name == null)
			return cardName.uniqueName();
		else
			return cardName.uniqueName() + "." + name;
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
	
	public int compareTo(TemplateName other) {
		int cs = 0;
		if (cardName != null && other.cardName == null)
			return -1;
		else if (cardName == null && other.cardName != null)
			return 1;
		else if (cardName != null && other.cardName != null)
			cs = cardName.compareTo(other.cardName);
		if (cs != 0)
			return cs;
		return name.compareTo(other.name);
	}

	@Override
	public <T extends NameOfThing> int compareTo(T other) {
		if (!(other instanceof TemplateName))
			return other.getClass().getName().compareTo(this.getClass().getName());
		return this.compareTo((TemplateName)other);
	}

}
