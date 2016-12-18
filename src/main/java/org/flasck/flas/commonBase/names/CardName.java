package org.flasck.flas.commonBase.names;

import org.flasck.flas.commonBase.NameOfThing;

public class CardName implements NameOfThing, Comparable<CardName> {
	public final PackageName pkg;
	public final String cardName;

	public CardName(PackageName pkg, String cardName) {
		this.pkg = pkg;
		this.cardName = cardName;
	}
	
	@Override
	public CardName containingCard() {
		return this;
	}

	public String uniqueName() {
		return pkg.uniqueName() + "." + cardName;
	}
	
	public String javaName() {
		if (pkg == null || pkg.simpleName() == null)
			return cardName;
		return pkg.simpleName() + "." + cardName;
	}

	@Override
	public String jsName() {
		if (pkg == null || pkg.simpleName() == null)
			return cardName;
		return pkg.simpleName() + "." + cardName;
	}

	public String jsUName() {
		if (pkg == null || pkg.simpleName() == null)
			return cardName;
		return pkg.simpleName() + "._" + cardName;
	}

	@Override
	public int compareTo(CardName o) {
		int pc = pkg.compareTo(o.pkg);
		if (pc != 0) return pc;
		return cardName.compareTo(o.cardName);
	}

	@Override
	public <T extends NameOfThing> int compareTo(T other) {
		if (!(other instanceof CardName))
			return other.getClass().getName().compareTo(this.getClass().getName());
		return this.compareTo((CardName)other);
	}

	public boolean isValid() {
		return pkg != null && cardName != null;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof CardName))
			return false;
		CardName o = (CardName) obj;
		return pkg.equals(o.pkg) && o.cardName.equals(cardName);
	}
	
	@Override
	public int hashCode() {
		return pkg.hashCode() ^ cardName.hashCode();
	}
}
