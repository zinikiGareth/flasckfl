package org.flasck.flas.commonBase.names;

import org.zinutils.exceptions.InvalidUsageException;

public class PackageName implements NameOfThing, Comparable<PackageName> {
	private final String name;
	private final Boolean builtin;

	// For the global namespace, things can either be "built in" or "stdlib"; either way name is null, but we can tell these apart as they are handled differently in a few places
	// (specifically, builtins don't have code generated for them)
	public PackageName(boolean isBuiltin) {
		this.name = null;
		this.builtin = isBuiltin;
	}

	public PackageName(String s) {
		if (s == null) {
			try {
				throw new InvalidUsageException("Must use PackageName(isBuiltin)");
			} catch (Exception ex) {
				ex.printStackTrace();
				throw ex;
			}
		}
		this.name = s;
		this.builtin = null;
	}

	@Override
	public NameOfThing container() {
		return null;
	}
	
	@Override
	public PackageName packageName() {
		return this;
	}

	@Override
	public String baseName() {
		return name;
	}

	@Override
	public NameOfThing containingCard() {
		return null;
	}
	
	public String uniqueName() {
		return name;
	}
	
	@Override
	public String jsName() {
		if (name == null)
			return null;
		return name.replace(".", "__");
	}

	@Override
	public String javaName() {
		return name;
	}

	@Override
	public String javaPackageName() {
		return name;
	}

	@Override
	public String javaClassName() {
		return name;
	}

	public String simpleName() {
		return name;
	}

	public String finalPart() {
		int idx = name.lastIndexOf('.');
		if (idx == -1)
			return name;
		else
			return name.substring(idx+1);
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof PackageName))
			return false;
		PackageName pn = (PackageName) obj;
		if (name == null && pn.name == null)
			return true;
		else if (name == null)
			return false;
		else
			return obj instanceof PackageName && name.equals(pn.name);
	}
	
	@Override
	public int hashCode() {
		if (name == null)
			return 0;
		return name.hashCode();
	}
	
	@Override
	public int compareTo(PackageName o) {
		if (name == null && o.name == null)
			return 0;
		else if (name == null)
			return -1;
		else if (o.name == null)
			return 1;
		else
			return name.compareTo(o.name);
	}
	
	@Override
	public <T extends NameOfThing> int compareTo(T other) {
		if (!(other instanceof PackageName))
			return other.getClass().getName().compareTo(this.getClass().getName());
		return this.compareTo((PackageName)other);
	}
	
	@Override
	public String toString() {
		return "Pkg[" + name + "]";
	}

	public boolean isBuiltin() {
		return builtin != null && builtin;
	}
}
