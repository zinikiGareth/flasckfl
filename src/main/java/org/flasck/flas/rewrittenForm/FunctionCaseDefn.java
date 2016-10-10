package org.flasck.flas.rewrittenForm;

import java.io.Serializable;
import java.io.Writer;

import org.flasck.flas.parsedForm.Scope.ScopeEntry;

@SuppressWarnings("serial")
public class FunctionCaseDefn implements Serializable {
	public final RWFunctionIntro intro;
	public final Object expr;

	public FunctionCaseDefn(ScopeEntry me, FunctionCaseDefn starter) {
		this.intro = starter.intro;
		this.expr = starter.expr;
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
