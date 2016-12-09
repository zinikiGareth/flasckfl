package org.flasck.flas.rewrittenForm;

public class CardName implements NameOfThing {
	private final PackageName pkg;
	private final String cardName;

	public CardName(PackageName pkg, String cardName) {
		this.pkg = pkg;
		this.cardName = cardName;
	}
	
	public static CardName none() {
		return new CardName(null, null);
	}

	@Override
	public String jsName() {
		if (pkg == null && cardName == null)
			return null;
		return pkg.simpleName() + "." + cardName;
	}
}
