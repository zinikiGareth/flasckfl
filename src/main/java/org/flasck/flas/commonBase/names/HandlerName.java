package org.flasck.flas.commonBase.names;

import org.zinutils.exceptions.NotImplementedException;

public class HandlerName implements NameOfThing, Comparable<NameOfThing> {
	public final NameOfThing name;
	public final String baseName;

	public HandlerName(NameOfThing n, String baseName) {
		this.name = n;
		this.baseName = baseName;
	}
	
	@Override
	public NameOfThing container() {
		return name;
	}

	@Override
	public String baseName() {
		return baseName;
	}
	
	@Override
	public NameOfThing containingCard() {
		return name.containingCard();
	}

	public String uniqueName() {
		if (name == null || name.uniqueName() == null || name.uniqueName().length() == 0)
			return baseName;
		else
			return name.uniqueName() + "." + baseName;
	}
	
	@Override
	public String javaName() {
		throw new NotImplementedException();
	}

	@Override
	public String javaClassName() {
		return name.uniqueName() + "$" + baseName;
	}

	@Override
	public int compareTo(NameOfThing o) {
		return uniqueName().compareTo(o.uniqueName());
	}

	@Override
	public String jsName() {
		if (name == null)
			return baseName;
		return name.jsName() + "." + baseName;
	}

	@Override
	public String jsUName() {
		if (name == null)
			return baseName;
		return name.jsName() + "._" + baseName;
	}

	@Override
	public String javaPackageName() {
		throw new org.zinutils.exceptions.NotImplementedException();
	}

	public PackageName packageName() {
		NameOfThing ret = name;
		while (ret != null) {
			if (ret instanceof PackageName)
				return (PackageName) ret;
			ret = ret.container();
		}
		throw new RuntimeException("No PackageName found");
	}

	@Override
	public String toString() {
		throw new NotImplementedException();
	}
}
