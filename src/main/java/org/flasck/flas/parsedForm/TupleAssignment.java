package org.flasck.flas.parsedForm;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.hsi.Slot;
import org.flasck.flas.lifting.NestedVarReader;
import org.flasck.flas.patterns.HSITree;
import org.flasck.flas.repository.HSICases;
import org.flasck.flas.repository.RepositoryEntry;
import org.flasck.flas.tc3.PolyInstance;
import org.flasck.flas.tc3.Type;
import org.zinutils.exceptions.NotImplementedException;

public class TupleAssignment implements RepositoryEntry, StandaloneDefn, TypeBinder, Comparable<StandaloneDefn> {
	public final List<LocatedName> vars;
	public final List<TupleMember> members = new ArrayList<TupleMember>();
	private FunctionName exprFnName;
	public final Expr expr;
	private NestedVarReader nestedVars;

	// We used located name here, not unresolvedvar, because this is defining the things
	public TupleAssignment(List<LocatedName> vars, FunctionName exprFnName, Expr expr) {
		this.vars = vars;
		this.exprFnName = exprFnName;
		this.expr = expr;
	}

	public FunctionName name() {
		return exprFnName;
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
		return "TupleAssignment{" + exprFnName.name + "=>" + vars.stream().map(ln -> ln.text).collect(Collectors.toList()) + "=" + expr + "}";
	}

	@Override
	public HSITree hsiTree() {
		throw new NotImplementedException();
	}

	@Override
	public HSICases hsiCases() {
		throw new NotImplementedException();
	}

	@Override
	public InputPosition location() {
		return vars.get(0).location;
	}

	@Override
	public void bindType(Type ty) {
		if (!(ty instanceof PolyInstance))
			throw new NotImplementedException();
		PolyInstance pi = (PolyInstance) ty;
		List<Type> tys = pi.getPolys();
		if (tys.size() != members.size())
			throw new NotImplementedException("Counts don't match");
		for (int i=0;i<members.size();i++) {
			TupleMember m = members.get(i);
			Type t = tys.get(i);
			m.bindType(t);
		}
	}

	@Override
	public Type type() {
		throw new NotImplementedException();
	}

	@Override
	public void nestedVars(NestedVarReader nestedVars) {
		this.nestedVars = nestedVars;
	}

	@Override
	public NestedVarReader nestedVars() {
		return nestedVars;
	}

	@Override
	public List<Slot> slots() {
		throw new NotImplementedException();
	}

	@Override
	public boolean isMyName(NameOfThing other) {
		throw new NotImplementedException();
	}

	@Override
	public int compareTo(StandaloneDefn o) {
		return name().compareTo(o.name());
	}

	public void addMember(TupleMember tm) {
		members.add(tm);
	}
}
