package org.flasck.flas.commonBase.names;

import org.zinutils.exceptions.UtilException;
import org.zinutils.xml.XMLElement;

public class AreaName implements NameOfThing, Comparable<AreaName> {
	private final String simple;
	public final CardName cardName;

	public AreaName(CardName cardName, String areaName) {
		this.cardName = cardName;
		this.simple = areaName;
	}
	
	public String getSimple() {
		return simple;
	}
	
	public String uniqueName() {
		return cardName.uniqueName() + "." + simple;
	}
	
	public String jsName() {
		return cardName.jsUName() + "." + simple;
	}

	public String jsUName() {
		if (cardName == null)
			return simple;
		return cardName.jsUName() + "." + simple;
	}

	public String javaName() {
		return cardName.javaName() + "." + simple;
	}
	
	@Override
	public String javaClassName() {
		return cardName.javaName() + "$" + simple;
	}

	public int compareTo(AreaName other) {
		int cs = 0;
		if (cardName != null && other.cardName == null)
			return -1;
		else if (cardName == null && other.cardName != null)
			return 1;
		else if (cardName != null && other.cardName != null)
			cs = cardName.compareTo(other.cardName);
		if (cs != 0)
			return cs;
		return simple.compareTo(other.simple);
	}

	@Override
	public <T extends NameOfThing> int compareTo(T other) {
		if (!(other instanceof AreaName))
			return other.getClass().getName().compareTo(this.getClass().getName());
		return this.compareTo((AreaName)other);
	}


	@Override
	public String toString() {
		throw new UtilException("Yo!");
	}

	@Override
	public CardName containingCard() {
		return cardName;
	}

	@Override
	public String writeToXML(XMLElement xe) {
		// TODO Auto-generated method stub
		return null;
	}
}
