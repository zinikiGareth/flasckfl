package org.flasck.flas.parsedForm;

import java.io.PrintWriter;
import java.util.List;
import java.util.stream.Collectors;

import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.repository.RepositoryEntry;

public class TupleAssignment implements RepositoryEntry {
	public final List<LocatedName> vars;
	private FunctionName exprFnName;
	public final Object expr;

	// We used located name here, not unresolvedvar, because this is defining the things
	public TupleAssignment(List<LocatedName> vars, FunctionName exprFnName, Object expr) {
		this.vars = vars;
		this.exprFnName = exprFnName;
		this.expr = expr;
	}

	public FunctionName exprFnName() {
		return exprFnName;
	}

	@Override
	public void dumpTo(PrintWriter pw) {
		pw.println(toString());
	}

	@Override
	public String toString() {
		return "TupleAssignment{" + vars.stream().map(ln -> ln.text).collect(Collectors.toList()) + "=" + expr + "}";
	}
}
