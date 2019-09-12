package org.flasck.flas.parsedForm;

import java.io.PrintWriter;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.AsString;
import org.flasck.flas.commonBase.Pattern;
import org.flasck.flas.commonBase.names.VarName;
import org.flasck.flas.repository.RepositoryEntry;

public class TypedPattern implements Pattern, AsString, RepositoryEntry {
	public final transient InputPosition typeLocation;
	public final TypeReference type;
	public final VarName var;

	public TypedPattern(InputPosition location, TypeReference type, VarName var) {
		this.typeLocation = location;
		this.type = type;
		this.var = var;
	}

	@Override
	public InputPosition location() {
		return typeLocation;
	}
	
	public VarName name() {
		return var;
	}
	
	@Override
	public void dumpTo(PrintWriter pw) {
		pw.println(toString());
	}

	@Override
	public String toString() {
		return "TypedPattern[" + type + ":" + var.var +"]";
	}

	@Override
	public String asString() {
		return "(" + type + " " + var.var + ")";
	}
}
