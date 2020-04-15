package org.flasck.flas.parsedForm;

import java.io.PrintWriter;

import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.commonBase.names.VarName;
import org.flasck.flas.repository.RepositoryEntry;

public class ObjectContract implements RepositoryEntry {
	private final VarName cv;
	private TypeReference implementing;

	public ObjectContract(TypeReference ctr, VarName cv) {
		this.implementing = ctr;
		this.cv = cv;
	}

	public NameOfThing varName() {
		return cv;
	}

	@Override
	public NameOfThing name() {
		return cv;
	}

	public TypeReference implementsType() {
		return implementing;
	}

	@Override
	public void dumpTo(PrintWriter pw) {
		pw.println("ObjectContract[" + cv.uniqueName() + "]");
	}

}
