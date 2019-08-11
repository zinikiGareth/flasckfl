package org.flasck.flas.repository;

import java.io.PrintWriter;

import org.flasck.flas.commonBase.names.SolidName;

public class BuiltinRepositoryEntry implements RepositoryEntry {
	private final SolidName name;

	public BuiltinRepositoryEntry(String string) {
		this.name = new SolidName(null, string);
	}

	@Override
	public void dumpTo(PrintWriter pw) {
		pw.println("Builtin[" + name + "]");
	}

	public void loadInto(Repository repository) {
		repository.addEntry(name, this);
	}

}
