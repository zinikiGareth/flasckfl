package org.flasck.flas.parsedForm;

import java.io.Writer;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Locatable;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.ScopeName;
import org.flasck.flas.vcode.hsieForm.HSIEForm.CodeType;
import org.zinutils.exceptions.UtilException;

public class FunctionCaseDefn implements ContainsScope, Locatable {
	public final FunctionIntro intro;
	public final Object expr;
	private Scope scope;
	private ScopeName caseName;

	public FunctionCaseDefn(FunctionName name, List<Object> args, Object expr) {
		intro = new FunctionIntro(name, args);
		if (expr == null)
			throw new UtilException("Cannot build function case with null expr");
		this.expr = expr;
	}

	@Override
	public Scope innerScope() {
		return scope;
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

	@Deprecated
	public String functionNameAsString() {
		return intro.name;
	}
	
	public FunctionName functionName() {
		return intro.name();
	}

	public void provideCaseName(int caseNum) {
		this.caseName = new ScopeName(this.intro.name().inContext, this.intro.name().name+"_"+caseNum);
		this.scope = new Scope(this.caseName);
	}

	@Deprecated
	public String caseNameAsString() {
		if (caseName == null)
			throw new UtilException("Asked for caseName when none provided");
		return caseName.jsName();
	}
	
	public ScopeName caseName() {
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
		return "FCD[" + intro.name + "/" + intro.args.size() + "]";
	}
}
