package org.flasck.flas.commonBase.names;

import org.flasck.flas.commonBase.NameOfThing;

public class ScopeName implements NameOfThing {
	private final String scope;

	public ScopeName(String scope) {
		this.scope = scope;
	}

	@Override
	public CardName containingCard() {
		return null; // TODO: this is wrong because this scope should be able to be within something else
	}

	@Override
	public String jsName() {
		return scope;
	}

	public static ScopeName none() {
		return new ScopeName(null);
	}

	public boolean isValid() {
		return scope != null;
	}

	public int compareTo(ScopeName sn) {
		if (scope == null && sn.scope == null)
			return 0;
		else if (scope == null)
			return -1;
		else
			return scope.compareTo(sn.scope);
	}

}
