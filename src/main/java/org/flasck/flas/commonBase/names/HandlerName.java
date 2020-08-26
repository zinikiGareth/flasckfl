package org.flasck.flas.commonBase.names;

import org.zinutils.exceptions.NotImplementedException;

public class HandlerName implements NameOfThing, Comparable<NameOfThing> {
	public final NameOfThing container;
	public final String baseName;

	public HandlerName(NameOfThing n, String baseName) {
		this.container = n;
		this.baseName = baseName;
	}
	
	@Override
	public NameOfThing container() {
		return container;
	}

	@Override
	public String baseName() {
		return baseName;
	}
	
	@Override
	public NameOfThing containingCard() {
		return container.containingCard();
	}

	public String uniqueName() {
		if (container == null || container.uniqueName() == null || container.uniqueName().length() == 0)
			return baseName;
		else
			return container.uniqueName() + "." + baseName;
	}
	
	@Override
	public String javaName() {
		return container.javaName() + "." + baseName;
	}

	@Override
	public String javaClassName() {
		return container.uniqueName() + "$" + baseName;
	}

	@Override
	public int compareTo(NameOfThing o) {
		return uniqueName().compareTo(o.uniqueName());
	}

	@Override
	public String jsName() {
		if (container == null)
			return baseName;
		return container.jsName() + "." + baseName;
	}

	@Override
	public String jsUName() {
		if (container == null)
			return baseName;
		return container.jsName() + "._" + baseName;
	}

	@Override
	public String javaPackageName() {
		throw new org.zinutils.exceptions.NotImplementedException();
	}

	public PackageName packageName() {
		NameOfThing ret = container;
		while (ret != null) {
			if (ret instanceof PackageName)
				return (PackageName) ret;
			ret = ret.container();
		}
		throw new RuntimeException("No PackageName found");
	}

	@Override
	public String toString() {
		return "Handler[" + this.uniqueName() + "]";
	}
}
