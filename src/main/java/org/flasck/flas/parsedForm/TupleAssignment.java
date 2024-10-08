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

public class TupleAssignment implements RepositoryEntry, LogicHolder, TypeBinder, Comparable<LogicHolder> {
	public final List<LocatedName> vars;
	public final List<TupleMember> members = new ArrayList<TupleMember>();
	private final FunctionName exprFnName;
	private final NameOfThing scopePackage;
	public final Expr expr;
	private NestedVarReader nestedVars;
	private FunctionConstness constNess;

	// We used located name here, not unresolvedvar, because this is defining the things
	public TupleAssignment(List<LocatedName> vars, FunctionName exprFnName, FunctionName scopePackage, Expr expr) {
		this.vars = vars;
		this.exprFnName = exprFnName;
		this.scopePackage = scopePackage;
		this.expr = expr;
	}

	@Override
	public boolean hasArgs() {
		return false;
	}
	
	@Override
	public boolean generate() {
		return true;
	}
	
	public FunctionName name() {
		return exprFnName;
	}

	public FunctionName exprFnName() {
		return exprFnName;
	}

	@Override
	public int argCount() {
		return 0; // should be able to have tuples in objects, would return 1
	}

	@Override
	public void setConstness(FunctionConstness fc) {
		this.constNess = fc;
	}
	
	@Override
	public FunctionConstness constNess() {
		return constNess;
	}

	@Override
	public boolean isObjAccessor() {
		return false;
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
		List<Type> tys = pi.polys();
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
	public boolean hasState() {
		return false;
	}
	
	@Override
	public StateHolder state() {
		return null;
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
	public int compareTo(LogicHolder o) {
		return name().compareTo(o.name());
	}

	public void addMember(TupleMember tm) {
		members.add(tm);
	}

	public NameOfThing scopePackage() {
		return scopePackage;
	}
}
