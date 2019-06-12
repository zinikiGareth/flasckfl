package org.flasck.flas.parsedForm;

import java.io.Writer;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.commonBase.Locatable;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.ScopeName;
import org.flasck.flas.parser.FunctionNameProvider;
import org.flasck.flas.vcode.hsieForm.HSIEForm.CodeType;
import org.zinutils.exceptions.UtilException;

public class FunctionCaseDefn implements Locatable, FunctionNameProvider {
	public final FunctionIntro intro;
	public final Expr guard;
	public final Object expr;
	private FunctionName caseName;

	@Deprecated
	public FunctionCaseDefn(FunctionName name, List<Object> args, Object expr) {
		intro = new FunctionIntro(name, args);
		this.guard = null;
		if (expr == null)
			throw new UtilException("Cannot build function case with null expr");
		this.expr = expr;
	}

	public FunctionCaseDefn(FunctionName name, List<Object> args, Expr guard, Expr expr) {
		intro = new FunctionIntro(name, args);
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
		return "FCD[" + intro.name().uniqueName() + "/" + intro.args.size() + (guard != null ? " guard": " default") + "]";
	}

	@Override
	public FunctionName functionName(InputPosition location, String base) {
		throw new org.zinutils.exceptions.NotImplementedException();
	}
}
