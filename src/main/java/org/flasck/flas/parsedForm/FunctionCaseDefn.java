package org.flasck.flas.parsedForm;

import java.io.Writer;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.commonBase.Locatable;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.parser.FunctionNameProvider;
import org.zinutils.exceptions.UtilException;

public class FunctionCaseDefn implements Locatable, FunctionNameProvider {
	public final FunctionIntro intro;
	public final Expr guard;
	public final Expr expr;
	public final InputPosition location;

	public FunctionCaseDefn(InputPosition location, FunctionIntro intro, Expr guard, Expr expr) {
		this.location = location;
		this.intro = intro;
		this.guard = guard;
		if (expr == null)
			throw new UtilException("Cannot build function case with null expr");
		this.expr = expr;
	}

	@Override
	public InputPosition location() {
		return location;
	}

	public int nargs() {
		return intro.args.size();
	}

	public FunctionName functionName() {
		return intro.name();
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
		return "FCD[" + (guard != null ? " guard" + guard : " default") + " = " + expr + "]";
	}

	@Override
	public FunctionName functionName(InputPosition location, String base) {
		throw new org.zinutils.exceptions.NotImplementedException();
	}
}
