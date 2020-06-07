package org.flasck.flas.commonBase.names;

import org.flasck.flas.blockForm.InputPosition;
import org.zinutils.exceptions.NotImplementedException;
import org.zinutils.exceptions.UtilException;

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
	public NameOfThing container() {
		return scope;
	}
	
	public PackageName packageName() {
		if (scope == null)
			return null;
		else
			return ((SolidName)scope).packageName();
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
	public String jsUName() {
		throw new UtilException("I don't think so");
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
	public String toString() {
		return "VarName[" + uniqueName() + "]";
	}
}
