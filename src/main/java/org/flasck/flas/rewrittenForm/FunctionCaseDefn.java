package org.flasck.flas.rewrittenForm;

import java.io.Serializable;
import java.io.Writer;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.parsedForm.Scope.ScopeEntry;
import org.zinutils.exceptions.UtilException;

@SuppressWarnings("serial")
public class FunctionCaseDefn implements ContainsScope, Serializable {
	public final FunctionIntro intro;
	public final Object expr;
	private final Scope scope;

	public FunctionCaseDefn(InputPosition location, String name, List<Object> args, Object expr) {
		intro = new FunctionIntro(location, name, args);
		if (expr == null)
			throw new UtilException("Cannot build function case with null expr");
		this.expr = expr;
		this.scope = null;
	}

	public FunctionCaseDefn(ScopeEntry me, FunctionCaseDefn starter) {
		this.intro = starter.intro;
		this.expr = starter.expr;
		this.scope = new Scope(me, this);
	}

	@Override
	public Scope innerScope() {
		if (scope == null)
			throw new UtilException("Can't do that with starter");
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
