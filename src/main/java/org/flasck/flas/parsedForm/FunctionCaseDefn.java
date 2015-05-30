package org.flasck.flas.parsedForm;

import java.util.List;

public class FunctionCaseDefn implements ContainsScope{
	public final FunctionIntro intro;
	public final Object expr;
	private final Scope scope;

	public FunctionCaseDefn(Scope inside, String name, List<Object> args, Object expr) {
		intro = new FunctionIntro(name, args);
		this.expr = expr;
		this.scope = new Scope(inside);
	}

	@Override
	public Scope innerScope() {
		return scope;
	}
	
	@Override
	public String toString() {
		return "FCD[" + intro.name + "/" + intro.args.size() + "]";
	}
}
