package org.flasck.flas.parsedForm;

import java.io.PrintWriter;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Locatable;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.hsi.Slot;
import org.flasck.flas.lifting.NestedVarReader;
import org.flasck.flas.patterns.HSITree;
import org.flasck.flas.repository.HSICases;
import org.flasck.flas.repository.RepositoryEntry;
import org.flasck.flas.tc3.Type;
import org.zinutils.exceptions.NotImplementedException;

public class TupleMember implements Locatable, RepositoryEntry, LogicHolder, Comparable<LogicHolder>, TypeBinder {
	private InputPosition location;
	public final TupleAssignment ta;
	public final int which;
	private final FunctionName myName;
	private NestedVarReader nestedVars;
	private Type type;
	private FunctionConstness constNess;

	public TupleMember(InputPosition location, TupleAssignment ta, int which, FunctionName myName) {
		this.location = location;
		this.ta = ta;
		this.which = which;
		this.myName = myName;
	}

	@Override
	public boolean hasArgs() {
		return false;
	}

	@Override
	public boolean generate() {
		return true;
	}
	
	@Override
	public InputPosition location() {
		return location;
	}

	public FunctionName exprFnName() {
		return ta.exprFnName();
	}

	public FunctionName name() {
		return myName;
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
	public HSITree hsiTree() {
		throw new NotImplementedException();
	}

	@Override
	public HSICases hsiCases() {
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

	@Override
	public void bindType(Type ty) {
		this.type = ty;
	}

	@Override
	public Type type() {
		return this.type;
	}

	@Override
	public void dumpTo(PrintWriter pw) {
		pw.println(toString());
	}
	
	@Override
	public String toString() {
		return "TupleMember[" + myName.uniqueName() + "]";
	}
}
