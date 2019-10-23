package org.flasck.flas.parsedForm;

import java.io.PrintWriter;
import java.util.Comparator;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Pattern;
import org.flasck.flas.commonBase.names.VarName;
import org.flasck.flas.repository.RepositoryEntry;
import org.flasck.flas.tc3.Type;

public class VarPattern implements Pattern, RepositoryEntry, WithTypeSignature {
	public static Comparator<VarPattern> comparator = new Comparator<VarPattern>() {
		@Override
		public int compare(VarPattern o1, VarPattern o2) {
			return o1.name().compareTo(o2.name());
		}
	};
	public final InputPosition varLoc;
	public final String var;
	private final VarName myName;
	private Type type;
	
	public VarPattern(InputPosition location, VarName name) {
		this.varLoc = location;
		this.var = name.var;
		this.myName = name;
	}

	@Override
	public String toString() {
		return myName.uniqueName();
	}

	@Override
	public InputPosition location() {
		return varLoc;
	}

	public VarName name() {
		return myName;
	}

	@Override
	public void dumpTo(PrintWriter pw) {
		pw.println("VarPattern[" + toString() + "]");
	}

	public void bindType(Type ty) {
		this.type = ty;
	}
	
	public Type type() {
		return type;
	}

	public String signature() {
		return type.signature();
	}

	public int argCount() {
		return type.argCount();
	}

	public Type get(int pos) {
		return type.get(pos);
	}

	public boolean incorporates(Type other) {
		return type.incorporates(other);
	}
}
