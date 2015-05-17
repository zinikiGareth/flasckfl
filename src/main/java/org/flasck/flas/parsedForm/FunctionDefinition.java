package org.flasck.flas.parsedForm;

import java.util.List;

public class FunctionDefinition {
	public final List<FunctionCaseDefn> cases;

	public FunctionDefinition(List<FunctionCaseDefn> defns) {
		this.cases = defns;
	}
	
	// FunctionDefinition also has nested Scope
}
