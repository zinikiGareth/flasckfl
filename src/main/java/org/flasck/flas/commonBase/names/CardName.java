package org.flasck.flas.commonBase.names;

import org.zinutils.exceptions.NotImplementedException;
import org.zinutils.xml.XMLElement;

public class CardName implements NameOfThing, Comparable<CardName> {
	public final PackageName pkg;
	public final String cardName;

	public CardName(PackageName pkg, String cardName) {
		this.pkg = pkg;
		this.cardName = cardName;
	}
	
	@Override
	public NameOfThing container() {
		return pkg;
	}
	
	@Override
	public PackageName packageName() {
		throw new NotImplementedException();
	}

	@Override
	public NameOfThing containingCard() {
		return this;
	}

	public String uniqueName() {
		if (pkg == null)
			return cardName;
		return pkg.uniqueName() + "." + cardName;
	}

	@Override
	public String javaPackageName() {
		return javaName();
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
	public String javaClassName() {
		return pkg.uniqueName() + "$" + cardName;
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

	@Override
	public String writeToXML(XMLElement xe) {
		// TODO Auto-generated method stub
		return null;
	}
}
