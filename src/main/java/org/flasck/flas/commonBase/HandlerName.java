package org.flasck.flas.commonBase;

import org.flasck.flas.commonBase.names.CardName;

public class HandlerName implements NameOfThing, Comparable<HandlerName> {
	public final NameOfThing name;
	public final String baseName;

	public HandlerName(NameOfThing n, String baseName) {
		this.name = n;
		this.baseName = baseName;
	}

	@Override
	public CardName containingCard() {
		return name.containingCard();
	}

	public String uniqueName() {
		if (name == null || name.uniqueName() == null || name.uniqueName().length() == 0)
			return baseName;
		else
			return name.uniqueName() + "." + baseName;
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
	public <T extends NameOfThing> int compareTo(T other) {
		if (!(other instanceof HandlerName))
			return other.getClass().getName().compareTo(this.getClass().getName());
		return this.compareTo((HandlerName)other);
	}

	@Override
	public String jsName() {
		if (name == null)
			return baseName;
		return name.jsName() + "." + baseName;
	}

}
