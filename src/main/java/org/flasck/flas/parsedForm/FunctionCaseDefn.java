package org.flasck.flas.parsedForm;

import java.io.Writer;
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
	
	public void dumpTo(Writer pw) throws Exception {
		pw.append(" ");
		for (Object o : intro.args) {
			pw.append(" ");
			pw.append(o.toString());
		}
		pw.append(" =\n");
		pw.append("    ");
		pw.append(expr.toString());
		pw.append("\n");
	}
	
	@Override
	public String toString() {
		return "FCD[" + intro.name + "/" + intro.args.size() + "]";
	}
}
