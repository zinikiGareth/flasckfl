package org.flasck.flas.parsedForm;

import java.io.PrintWriter;

import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.commonBase.names.SolidName;
import org.flasck.flas.repository.RepositoryEntry;

public class CurryArgument implements RepositoryEntry {

	@Override
	public NameOfThing name() {
		return new SolidName(null, "_");
	}

	@Override
	public void dumpTo(PrintWriter pw) {
		pw.println("_curry_");
	}
}
