package org.flasck.flas.parsedForm;

import java.util.List;

public class FunctionDefinition {
	public final String name;
	public final int nargs;
	public final List<FunctionCaseDefn> cases;

	public FunctionDefinition(String name, int nargs, List<FunctionCaseDefn> list) {
		this.name = name;
		this.nargs = nargs;
		this.cases = list;
	}

	public FunctionDefinition(FunctionIntro intro, List<FunctionCaseDefn> list) {
		this.name = intro.name;
		this.nargs = intro.args.size();
		this.cases = list;
	}
	
	// FunctionDefinition also has nested Scope
}
