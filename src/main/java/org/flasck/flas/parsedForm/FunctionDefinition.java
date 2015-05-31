package org.flasck.flas.parsedForm;

import java.util.List;

public class FunctionDefinition {
	public final String name;
	public final int nargs;
	public final List<FunctionCaseDefn> cases;
//	private final Scope scope;

	public FunctionDefinition(String name, int nargs, List<FunctionCaseDefn> list) {
		this.name = name;
		this.nargs = nargs;
		this.cases = list;
//		for (int i=0;i<list.size()-1;i++)
//			if (list.get(i).innerScope().size() > 0)
//				throw new UtilException("Can only attach nested definitions to last case");
//		this.scope = list.get(list.size()-1).innerScope();
	}

	public FunctionDefinition(FunctionIntro intro, List<FunctionCaseDefn> list) {
		this(intro.name, intro.args.size(), list);
	}
}
