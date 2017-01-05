package org.flasck.flas.commonBase.names;

import org.flasck.flas.blockForm.InputPosition;

public class VarName implements NameOfThing, Comparable<VarName> {
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

	public String uniqueName() {
		return scope.uniqueName() + "." + var;
	}
	
	@Override
	public String jsName() {
		return scope.jsName() + "." + var;
	}
	
	public int compareTo(VarName other) {
		int cs = 0;
		if (scope != null && other.scope == null)
			return -1;
		else if (scope == null && other.scope != null)
			return 1;
		else if (scope != null && other.scope != null)
			cs = scope.compareTo(other.scope);
		if (cs != 0)
			return cs;
		return var.compareTo(other.var);
	}

	@Override
	public <T extends NameOfThing> int compareTo(T other) {
		if (!(other instanceof VarName))
			return other.getClass().getName().compareTo(this.getClass().getName());
		return this.compareTo((VarName)other);
	}
}
