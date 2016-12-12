package org.flasck.flas.rewrittenForm;

import java.io.Writer;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.flasck.flas.rewriter.GatherScopedVars;

public class RWFunctionCaseDefn {
	private final RWFunctionIntro intro;
	public final Object expr;
	private final int csNo;

	public RWFunctionCaseDefn(RWFunctionIntro intro, int csNo, Object expr) {
		this.intro = intro;
		this.csNo = csNo;
		this.expr = expr;
	}
	
	public String caseName() {
		return intro.fnName.jsName() + "_" + csNo;
	}
	
	public void gatherScopedVars(Set<ScopedVar> scopedVars) {
		new GatherScopedVars(scopedVars).dispatch(expr);
	}
	
	public Set<String> varNames() {
		return intro.vars.keySet();
	}

	public Collection<LocalVar> vars() {
		return intro.vars.values();
	}
	
	public List<Object> args() {
		return intro.args;
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
		return "FCD[" + intro.fnName.jsName() + "/" + intro.args.size() + "]";
	}
}
