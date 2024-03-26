package org.flasck.flas.commonBase.names;

import org.flasck.jvm.J;
import org.zinutils.exceptions.InvalidUsageException;

public class SolidName implements NameOfThing, Comparable<NameOfThing> {
	protected final NameOfThing container;
	protected final String name;

	public SolidName(NameOfThing container, String name) {
		if (container == null)
			throw new InvalidUsageException("container must be at least a null package");
		this.container = container;
		this.name = name;
	}
	
	@Override
	public NameOfThing container() {
		return container;
	}

	public String baseName() {
		return name;
	}
	
	public String uniqueName() {
		if (container == null || container.uniqueName() == null || container.uniqueName().length() == 0)
			return name;
		else
			return container.uniqueName() + "." + name;
	}
	
	@Override
	public String jsName() {
		if (container == null || container.jsName() == null || container.jsName().length() == 0)
			return name;
		return container.jsName() + "." + name;
	}

	public String javaPackageName() {
		if (container == null)
			return null;
		return container.javaName();
	}
	
	@Override
	public String javaName() {
		if (container == null || (container instanceof PackageName && ((PackageName)container).baseName() == null)) {
			if (name.equals("Entity"))
				return J.JVM_FIELDS_CONTAINER_WRAPPER;
			else
				return J.BUILTINPKG + "." + name;
		} else
			return uniqueName();
	}

	@Override
	public String javaClassName() {
		if (container == null) {
			if (name.equals("Entity"))
				return J.JVM_FIELDS_CONTAINER_WRAPPER;
			String use = name;
			if ("Error".equals(name))
				use = "FLError";
			return J.BUILTINPKG + "." + use;
		} else if (container instanceof PackageName)
			return container.uniqueName() + "." + name;
		else
			return container.uniqueName() + "$" + name;
	}

	@Override
	public NameOfThing containingCard() {
		if (container == null)
			return null;
		return container.containingCard();
	}

	public int compareTo(NameOfThing other) {
		return uniqueName().compareTo(other.uniqueName());
	}

	public PackageName packageName() {
		NameOfThing ret = container;
		while (ret != null) {
			if (ret instanceof PackageName)
				return (PackageName) ret;
			ret = ret.container();
		}
		return new PackageName(null);
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof SolidName && uniqueName().equals(((SolidName) obj).uniqueName());
	}
	
	@Override
	public int hashCode() {
		return uniqueName().hashCode();
	}
	
	@Override
	public String toString() {
		return uniqueName();
	}
}
