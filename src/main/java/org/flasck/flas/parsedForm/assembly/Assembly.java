package org.flasck.flas.parsedForm.assembly;

import java.io.PrintWriter;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.AssemblyName;
import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.repository.RepositoryEntry;

public class Assembly implements RepositoryEntry {
	protected final AssemblyName assemblyName;
	private final InputPosition location;

	public Assembly(InputPosition loc, AssemblyName assemblyName) {
		this.location = loc;
		this.assemblyName = assemblyName;
	}

	@Override
	public AssemblyName name() {
		return assemblyName;
	}

	@Override
	public InputPosition location() {
		return location;
	}

	@Override
	public void dumpTo(PrintWriter pw) {
		pw.println("assembly[" + assemblyName.uniqueName() + "]");
	}
}
