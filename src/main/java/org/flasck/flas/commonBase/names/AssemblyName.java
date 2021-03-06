package org.flasck.flas.commonBase.names;

import org.zinutils.exceptions.NotImplementedException;

public class AssemblyName implements NameOfThing {
	private final NameOfThing pkg;
	private final String name;

	public AssemblyName(NameOfThing pkg, String name) {
		this.pkg = pkg;
		this.name = name;
	}

	@Override
	public String uniqueName() {
		if (name != null)
			return pkg.uniqueName() + "_" + name;
		else
			return pkg.uniqueName();
	}
	
	@Override
	public String baseName() {
		return name;
	}

	@Override
	public String jsName() {
		return pkg.jsName();
	}

	@Override
	public String javaName() {
		return pkg.javaName();
	}

	@Override
	public String javaPackageName() {
		throw new NotImplementedException();
	}

	@Override
	public String javaClassName() {
		throw new NotImplementedException();
	}

	@Override
	public NameOfThing container() {
		return pkg;
	}

	@Override
	public NameOfThing containingCard() {
		throw new NotImplementedException();
	}

	@Override
	public <T extends NameOfThing> int compareTo(T other) {
		throw new NotImplementedException();
	}

	@Override
	public PackageName packageName() {
		return pkg.packageName();
	}
}
