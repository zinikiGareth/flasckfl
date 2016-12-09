package org.flasck.flas.rewrittenForm;

public class CSName implements NameOfThing {
	private final CardName card;
	private final String cname;

	public CSName(CardName card, String cname) {
		this.card = card;
		this.cname = cname;
	}
	
	@Override
	public String jsName() {
		return card.jsName() + "." + cname;
	}

}
