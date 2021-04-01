package org.flasck.flas.parsedForm.st;

import java.io.PrintWriter;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.commonBase.names.VarName;
import org.flasck.flas.repository.RepositoryEntry;

public class MockApplication implements RepositoryEntry {
	private final VarName vn;

	public MockApplication(VarName vn) {
		this.vn = vn;
	}

	@Override
	public NameOfThing name() {
		return vn;
	}

	@Override
	public InputPosition location() {
		return vn.loc;
	}
	
	public String asVar() {
		return vn.var;
	}

	@Override
	public void dumpTo(PrintWriter pw) {
		pw.println("MockApplication " + vn.uniqueName());
	}

}
