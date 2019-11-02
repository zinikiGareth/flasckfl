package org.flasck.flas.parsedForm;

import java.io.PrintWriter;

import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.lifting.NestedVarReader;
import org.flasck.flas.repository.RepositoryEntry;
import org.flasck.flas.tc3.Type;

public class StandaloneMethod implements RepositoryEntry, StandaloneDefn, Comparable<StandaloneDefn> {
	public final ObjectMethod om;
	private NestedVarReader nestedVars;

	public StandaloneMethod(ObjectMethod om) {
		this.om = om;
	}

	public FunctionName name() {
		return om.name();
	}
	
	public void bindType(Type ty) {
		om.bindType(ty);
	}
	
	public Type type() {
		return om.type();
	}

	public void nestedVars(NestedVarReader nestedVars) {
		this.nestedVars = nestedVars;
	}

	public NestedVarReader nestedVars() {
		return nestedVars;
	}

	@Override
	public void dumpTo(PrintWriter pw) {
		pw.println(toString());
	}

	@Override
	public int compareTo(StandaloneDefn o) {
		return name().compareTo(o.name());
	}

	@Override
	public String toString() {
		return "StandaloneMethod[" + om + "]";
	}
}
