package org.flasck.flas.parsedForm;

import java.io.Writer;
import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.commonBase.Locatable;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.parser.FunctionNameProvider;
import org.flasck.flas.vcode.hsieForm.HSIEForm.CodeType;
import org.zinutils.exceptions.UtilException;

public class FunctionCaseDefn implements Locatable, FunctionNameProvider {
	public final FunctionIntro intro;
	public final Expr guard;
	public final Expr expr;
	private FunctionName caseName;

	public FunctionCaseDefn(Expr guard, Expr expr) {
		this.intro = null;
		this.guard = guard;
		if (expr == null)
			throw new UtilException("Cannot build function case with null expr");
		this.expr = expr;
	}

	@Override
	public InputPosition location() {
		return intro.location;
	}

	public CodeType mytype() {
		return intro.name().codeType;
	}

	public int nargs() {
		return intro.args.size();
	}

	public FunctionName functionName() {
		return intro.name();
	}

	public void provideCaseName(FunctionName name) {
		this.caseName = name;
	}

	public FunctionName caseName() {
		if (caseName == null)
			throw new UtilException("Asked for caseName when none provided");
		return caseName;
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
