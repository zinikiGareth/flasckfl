package org.flasck.flas.parsedForm;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.repository.RepositoryEntry;

public class FunctionDefinition implements RepositoryEntry {
	private final FunctionName name;
	private final int nargs;
	private final List<FunctionIntro> intros = new ArrayList<>();

	public FunctionDefinition(FunctionName name, int nargs) {
		this.name = name;
		this.nargs = nargs;
	}
	
	public void intro(FunctionIntro next) {
		this.intros.add(next);
	}

	public FunctionName name() {
		return name;
	}
	
	public int argCount() {
		return nargs;
	}
	
	public List<FunctionIntro> intros() {
		return intros;
	}
	
	@Override
	public void dumpTo(PrintWriter pw) {
		throw new org.zinutils.exceptions.NotImplementedException();
	}

	@Override
	public String toString() {
		return "FunctionDefinition[" + name.uniqueName() + "/" + nargs + "{" + intros.size() + "}]";
	}
}
