package org.flasck.flas.commonBase.names;

import org.zinutils.exceptions.NotImplementedException;

public class CSName implements NameOfThing, Comparable<CSName> {
	private final CardName card;
	private final String cname;

	public CSName(CardName card, String cname) {
		this.card = card;
		this.cname = cname;
	}
	
	@Override
	public NameOfThing container() {
		return card;
	}
	
	public NameOfThing containingCard() {
		return card;
	}

	public String baseName() {
		return cname;
	}
	
	public String uniqueName() {
		return card.uniqueName() + "." + cname;
	}
	
	@Override
	public String jsName() {
		return card.jsName() + "." + cname;
	}

	@Override
	public String jsUName() {
		return card.jsName() + "._" + cname;
	}
	
	@Override
	public String javaName() {
		throw new NotImplementedException("I think you should be using javaClassName here");
	}

	@Override
	public String javaClassName() {
		return card.javaName() + "$" + cname;
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
	public String javaPackageName() {
		throw new org.zinutils.exceptions.NotImplementedException();
	}

	public PackageName packageName() {
		NameOfThing ret = card;
		while (ret != null) {
			if (ret instanceof PackageName)
				return (PackageName) ret;
			ret = ret.container();
		}
		throw new RuntimeException("No PackageName found");
	}
	
}
