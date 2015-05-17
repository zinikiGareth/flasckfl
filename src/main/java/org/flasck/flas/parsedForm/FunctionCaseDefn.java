package org.flasck.flas.parsedForm;

import java.util.List;

public class FunctionCaseDefn {
	public final String name;
	public final List<Object> args;
	public final Object expr;

	public FunctionCaseDefn(String name, List<Object> args, Object expr) {
		this.name = name;
		this.args = args;
		this.expr = expr;
	}
	
	@Override
	public String toString() {
		return "FCD[" + name + "/" + args.size() + "]";
	}
}
