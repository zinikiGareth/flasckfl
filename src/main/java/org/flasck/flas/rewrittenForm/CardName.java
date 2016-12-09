package org.flasck.flas.rewrittenForm;

public class CardName implements NameOfThing {
	private final PackageName pkg;
	private final String cardName;

	public CardName(PackageName pkg, String cardName) {
		this.pkg = pkg;
		this.cardName = cardName;
	}

	@Override
	public String jsName() {
		return pkg.simpleName() + "." + cardName;
	}
}
