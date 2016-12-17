package org.flasck.flas.commonBase.names;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.NameOfThing;

public class VarName implements NameOfThing {
	public final InputPosition loc;
	public final NameOfThing scope;
	public final String var;

	public VarName(InputPosition loc, NameOfThing name, String var) {
		this.loc = loc;
		this.scope = name;
		this.var = var;
	}
	
	@Override
	public CardName containingCard() {
		return scope.containingCard();
	}

	@Override
	public String jsName() {
		return scope.jsName() + "." + var;
	}
}
