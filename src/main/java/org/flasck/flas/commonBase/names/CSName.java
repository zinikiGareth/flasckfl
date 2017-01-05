package org.flasck.flas.commonBase.names;

import org.zinutils.xml.XMLElement;

public class CSName implements NameOfThing, Comparable<CSName> {
	private final CardName card;
	private final String cname;

	public CSName(CardName card, String cname) {
		this.card = card;
		this.cname = cname;
	}
	
	public CardName containingCard() {
		return card;
	}
	
	public String uniqueName() {
		return card.uniqueName() + "." + cname;
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

	@Override
	public <T extends NameOfThing> int compareTo(T other) {
		if (!(other instanceof CSName))
			return other.getClass().getName().compareTo(this.getClass().getName());
		return this.compareTo((CSName)other);
	}

	@Override
	public String writeToXML(XMLElement xe) {
		// TODO Auto-generated method stub
		return null;
	}

}
