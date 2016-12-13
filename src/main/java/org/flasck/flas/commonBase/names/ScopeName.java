package org.flasck.flas.commonBase.names;

import org.flasck.flas.commonBase.NameOfThing;

public class ScopeName implements NameOfThing {
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

	public int compareTo(ScopeName sn) {
		if (myname == null && sn.myname == null)
			return 0;
		else if (myname == null)
			return -1;
		else
			return myname.compareTo(sn.myname);
	}

}
