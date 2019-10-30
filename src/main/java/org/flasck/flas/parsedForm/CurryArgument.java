package org.flasck.flas.parsedForm;

import java.io.PrintWriter;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Locatable;
import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.commonBase.names.SolidName;
import org.flasck.flas.repository.RepositoryEntry;

public class CurryArgument implements RepositoryEntry, Locatable {
	private final InputPosition pos;

	public CurryArgument(InputPosition pos) {
		this.pos = pos;
	}

	@Override
	public InputPosition location() {
		return pos;
	}

	@Override
	public NameOfThing name() {
		return new SolidName(null, "_");
	}

	@Override
	public void dumpTo(PrintWriter pw) {
		pw.println("_curry_");
	}
}
