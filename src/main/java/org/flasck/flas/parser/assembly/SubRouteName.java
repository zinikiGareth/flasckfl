package org.flasck.flas.parser.assembly;

import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.commonBase.names.PackageName;
import org.zinutils.exceptions.NotImplementedException;

public class SubRouteName implements NameOfThing {
	private final NameOfThing name;
	private final String s;

	public SubRouteName(NameOfThing name, String s) {
		this.name = name;
		this.s = s;
	}

	@Override
	public String baseName() {
		throw new NotImplementedException();
	}

	@Override
	public String uniqueName() {
		throw new NotImplementedException();
	}

	@Override
	public String jsName() {
		throw new NotImplementedException();
	}

	@Override
	public String javaName() {
		throw new NotImplementedException();
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
		throw new NotImplementedException();
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
		throw new NotImplementedException();
	}
	
	@Override
	public int hashCode() {
		return name.hashCode() ^ s.hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof SubRouteName))
			return false;
		SubRouteName srn = (SubRouteName) obj;
		return srn.name.equals(name) && srn.s.equals(s);
	}
}
