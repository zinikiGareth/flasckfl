package org.flasck.flas.commonBase.names;

import org.flasck.flas.commonBase.NameOfThing;

public class ScopeName implements NameOfThing, Comparable<ScopeName> {
	private final NameOfThing inside;
	private final String myname;

	public ScopeName(NameOfThing inside, String myname) {
		this.inside = inside;
		this.myname = myname;
	}

	@Override
	public CardName containingCard() {
		return inside.containingCard();
	}

	@Override
	public String jsName() {
		return (inside == null?myname:inside.jsName()+"."+myname);
	}

	public int compareTo(ScopeName other) {
		int cs = 0;
		if (inside != null && other.inside == null)
			return -1;
		else if (inside == null && other.inside != null)
			return 1;
		else if (inside != null && other.inside != null)
			cs = inside.compareTo(other.inside);
		if (cs != 0)
			return cs;
		if (myname == null && other.myname == null)
			return 0;
		else if (myname == null)
			return -1;
		else
			return myname.compareTo(other.myname);
	}

	@Override
	public <T extends NameOfThing> int compareTo(T other) {
		if (!(other instanceof ScopeName))
			return other.getClass().getName().compareTo(this.getClass().getName());
		return this.compareTo((ScopeName)other);
	}

}
