package org.flasck.flas.tc3;

import java.io.PrintWriter;

import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.commonBase.names.SolidName;
import org.flasck.flas.repository.RepositoryEntry;
import org.zinutils.exceptions.NotImplementedException;

public class Primitive implements RepositoryEntry, Type {
	private final SolidName name;

	public Primitive(String name) {
		this.name = new SolidName(null, name);
	}

	@Override
	public String signature() {
		return this.name.baseName();
	}

	@Override
	public int argCount() {
		throw new NotImplementedException();
	}

	@Override
	public Type get(int pos) {
		throw new NotImplementedException();
	}

	@Override
	public NameOfThing name() {
		return name;
	}

	@Override
	public void dumpTo(PrintWriter pw) {
		pw.print(name.baseName());
	}
	
	@Override
	public String toString() {
		return "Primitive[" + name.uniqueName() + "]";
	}
}
