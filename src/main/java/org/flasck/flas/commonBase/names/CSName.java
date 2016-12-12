package org.flasck.flas.commonBase.names;

import org.flasck.flas.commonBase.NameOfThing;

public class CSName implements NameOfThing, Comparable<CSName> {
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

	@Override
	public int compareTo(CSName o) {
		int cc = card.compareTo(o.card);
		if (cc != 0) return cc;
		return cname.compareTo(o.cname);
	}

}
