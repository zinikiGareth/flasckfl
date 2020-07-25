package org.flasck.flas.parsedForm;

import java.io.PrintWriter;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.AsString;
import org.flasck.flas.commonBase.Pattern;
import org.flasck.flas.commonBase.names.VarName;
import org.flasck.flas.repository.RepositoryEntry;
import org.flasck.flas.tc3.Type;

public class TypedPattern implements Pattern, AsString, RepositoryEntry {
	public final transient InputPosition typeLocation;
	public final TypeReference type;
	public final VarName var;
	private LogicHolder definedBy;

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
	
	public LogicHolder definedBy() {
		return definedBy;
	}

	public void isDefinedBy(LogicHolder definedBy) {
		this.definedBy = definedBy;
	}

	@Override
	public void dumpTo(PrintWriter pw) {
		pw.println(toString());
	}

	public Type type() {
		try {
			return (Type) type.defn();
		} catch (ClassCastException ex) {
			System.out.println("Error with bound defn of " + type.name());
			throw ex;
		}
	}
	
	@Override
	public String toString() {
		return "TypedPattern[" + type + ":" + var.uniqueName() +"]";
	}

	@Override
	public String asString() {
		return "(" + type + " " + var.var + ")";
	}
}
