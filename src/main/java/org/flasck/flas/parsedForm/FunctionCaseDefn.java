package org.flasck.flas.parsedForm;

import java.util.List;

public class FunctionCaseDefn {
	public final FunctionIntro intro;
	public final Object expr;

	public FunctionCaseDefn(String name, List<Object> args, Object expr) {
		intro = new FunctionIntro(name, args);
		this.expr = expr;
	}
	
	@Override
	public String toString() {
		return "FCD[" + intro.name + "/" + intro.args.size() + "]";
	}
}
