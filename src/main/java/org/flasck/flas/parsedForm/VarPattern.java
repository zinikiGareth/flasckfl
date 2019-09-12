package org.flasck.flas.parsedForm;

import java.io.PrintWriter;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Pattern;
import org.flasck.flas.commonBase.names.VarName;
import org.flasck.flas.repository.RepositoryEntry;

public class VarPattern implements Pattern, RepositoryEntry {
	public final InputPosition varLoc;
	public final String var;
	private final VarName myName;
	
	public VarPattern(InputPosition location, VarName name) {
		this.varLoc = location;
		this.var = name.var;
		this.myName = name;
	}

	@Override
	public String toString() {
		return myName.uniqueName();
	}

	@Override
	public InputPosition location() {
		return varLoc;
	}

	public VarName name() {
		return myName;
	}

	@Override
	public void dumpTo(PrintWriter pw) {
		pw.println("VarPattern[" + toString() + "]");
	}
}
