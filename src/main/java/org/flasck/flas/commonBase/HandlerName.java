package org.flasck.flas.commonBase;

import org.flasck.flas.commonBase.names.CardName;

public class HandlerName implements NameOfThing, Comparable<HandlerName> {
	private final NameOfThing name;
	private String baseName;

	public HandlerName(NameOfThing n, String baseName) {
		this.name = n;
		this.baseName = baseName;
	}

	@Override
	public CardName containingCard() {
		return name.containingCard();
	}

	@SuppressWarnings("unchecked")
	@Override
	public int compareTo(HandlerName o) {
		int cc = 0;
		if (name != null && o.name == null)
			return -1;
		else if (name == null && o.name != null)
			return 1;
		else if (name != null && o.name != null)
			cc = ((Comparable<NameOfThing>)name).compareTo(o.name);
		if (cc != 0)
			return cc;
		return baseName.compareTo(o.baseName);
	}

	@Override
	public String jsName() {
		return name.jsName() + "." + baseName;
	}

}
