package org.flasck.flas.commonBase;

import org.flasck.flas.commonBase.names.CardName;
import org.flasck.flas.commonBase.names.ScopeName;
import org.zinutils.exceptions.UtilException;

public class HandlerName implements NameOfThing, Comparable<HandlerName> {
	private final CardName cn;
	private ScopeName sn;
	private String baseName;

	public HandlerName(CardName cn, ScopeName sn, String baseName) {
		this.cn = cn.isValid()?cn:null;
		this.sn = sn.isValid()?sn:null;
		if (this.cn == null && this.sn == null)
			throw new UtilException("Must have cn or sn");
		this.baseName = baseName;
	}

	@Override
	public CardName containingCard() {
		return cn;
	}

	@Override
	public int compareTo(HandlerName o) {
		int cc;
		if (cn != null && o.cn == null)
			return -1;
		else if (cn == null && o.cn != null)
			return 1;
		else if (cn != null && o.cn != null)
			cc = cn.compareTo(o.cn);
		else
			cc = sn.compareTo(o.sn);
		if (cc != 0)
			return cc;
		return baseName.compareTo(o.baseName);
	}

	@Override
	public String jsName() {
		if (cn != null)
			return cn.jsName() + "." + baseName;
		else
			return sn.jsName() + "." + baseName;
	}

}
