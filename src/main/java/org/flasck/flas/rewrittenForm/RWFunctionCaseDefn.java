package org.flasck.flas.rewrittenForm;

import java.io.Serializable;
import java.io.Writer;

@SuppressWarnings("serial")
public class RWFunctionCaseDefn implements Serializable {
	public final RWFunctionIntro intro;
	public final Object expr;

	public RWFunctionCaseDefn(RWFunctionIntro intro, Object expr) {
		this.intro = intro;
		this.expr = expr;
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
