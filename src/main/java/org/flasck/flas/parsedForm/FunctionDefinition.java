package org.flasck.flas.parsedForm;

import java.util.List;

public class FunctionDefinition {
	public final List<FunctionCaseDefn> cases;
	public final int nargs;

	public FunctionDefinition(int nargs, List<FunctionCaseDefn> defns) {
		this.nargs = nargs;
		this.cases = defns;
	}
	
	// FunctionDefinition also has nested Scope
}
