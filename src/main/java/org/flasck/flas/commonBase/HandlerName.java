package org.flasck.flas.commonBase;

import org.flasck.flas.commonBase.names.CardName;
import org.flasck.flas.commonBase.names.ScopeName;
import org.zinutils.exceptions.UtilException;

public class HandlerName implements NameOfThing, Comparable<HandlerName> {
	// TODO: reduce these down to one
	private final NameOfThing name;
	private String baseName;

	public HandlerName(CardName cn, ScopeName sn, String baseName) {
		this.name = cn.isValid()?cn:sn.isValid()?sn:null;
		if (this.name == null)
			throw new UtilException("Must have cn or sn");
		this.baseName = baseName;
	}

	public HandlerName(NameOfThing n, String baseName) {
		this.name = n;
		this.baseName = baseName;
	}

	@Override
	public CardName containingCard() {
		return name.containingCard();
	}

	@SuppressWarnings("unchecked")
	@Override
	public int compareTo(HandlerName o) {
		int cc = 0;
		if (name != null && o.name == null)
			return -1;
		else if (name == null && o.name != null)
			return 1;
		else if (name != null && o.name != null)
			cc = ((Comparable<NameOfThing>)name).compareTo(o.name);
		if (cc != 0)
			return cc;
		return baseName.compareTo(o.baseName);
	}

	@Override
	public String jsName() {
		return name.jsName() + "." + baseName;
	}

}
