package org.flasck.flas.commonBase.names;

import org.flasck.flas.commonBase.NameOfThing;

public class CardName implements NameOfThing, Comparable<CardName> {
	public final PackageName pkg;
	public final String cardName;

	public CardName(PackageName pkg, String cardName) {
		this.pkg = pkg;
		this.cardName = cardName;
	}
	
	public static CardName none() {
		return new CardName(null, null);
	}
	
	@Override
	public CardName containingCard() {
		return this;
	}

	public String javaName() {
		if (pkg == null && cardName == null)
			return null;
		if (pkg == null || pkg.simpleName() == null)
			return cardName;
		return pkg.simpleName() + "." + cardName;
	}

	@Override
	public String jsName() {
		if (pkg == null && cardName == null)
			return null;
		if (pkg == null || pkg.simpleName() == null)
			return cardName;
		return pkg.simpleName() + "." + cardName;
	}

	// I think this is the whole reason we're doing this ...
	public String jsUName() {
		if (pkg == null && cardName == null)
			return null;
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

	public boolean isValid() {
		return pkg != null && cardName != null;
	}
}
