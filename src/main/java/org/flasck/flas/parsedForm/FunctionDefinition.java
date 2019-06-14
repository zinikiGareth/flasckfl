package org.flasck.flas.parsedForm;

import org.flasck.flas.commonBase.names.FunctionName;

public class FunctionDefinition {
	private final FunctionName name;
	private final int nargs;

	public FunctionDefinition(FunctionName name, int nargs) {
		this.name = name;
		this.nargs = nargs;
	}

	public FunctionName getName() {
		return name;
	}
	
	public int getArgCount() {
		return nargs;
	}

	@Override
	public String toString() {
		return "FunctionDefinition[]";
	}
}
