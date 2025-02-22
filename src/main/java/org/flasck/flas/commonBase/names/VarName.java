package org.flasck.flas.commonBase.names;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Locatable;
import org.zinutils.exceptions.NotImplementedException;

public class VarName implements NameOfThing, Locatable, Comparable<VarName> {
	public final InputPosition loc;
	public final NameOfThing scope;
	public final String var;

	public VarName(InputPosition loc, NameOfThing name, String var) {
		this.loc = loc;
		this.scope = name;
		this.var = var;
	}
	
	@Override
	public InputPosition location() {
		return loc;
	}
	
	@Override
	public NameOfThing container() {
		return scope;
	}
	
	public PackageName packageName() {
		if (scope == null)
			return null;
		else
			return scope.packageName();
	}
	
	@Override
	public String baseName() {
		return var;
	}
	
	@Override
	public NameOfThing containingCard() {
		return scope.containingCard();
	}

	public String uniqueName() {
		return scope.uniqueName() + "." + var;
	}
	
	@Override
	public String jsName() {
		return scope.jsName() + "." + var;
	}

	@Override
	public String javaName() {
		throw new NotImplementedException();
	}

	@Override
	public String javaClassName() {
		return scope.uniqueName() + "$" + var;
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

	@Override
	public String javaPackageName() {
		throw new org.zinutils.exceptions.NotImplementedException();
	}

	@Override
	public int hashCode() {
		return scope.hashCode() ^ var.hashCode();
	}
	
	@Override
	public boolean equals(Object other) {
		if (!(other instanceof VarName))
			return false;
		VarName o = (VarName) other;
		return o.scope.equals(scope) && o.var.equals(var);
	}
	@Override
	public String toString() {
		return "VarName[" + uniqueName() + "]";
	}
}
