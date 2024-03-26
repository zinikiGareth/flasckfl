package org.flasck.flas.tc3;

import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.commonBase.names.NamedThing;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.commonBase.names.SolidName;
import org.flasck.flas.parsedForm.ContractDecl;
import org.flasck.flas.parsedForm.FieldsDefn.FieldsType;
import org.flasck.flas.parsedForm.HandlerImplements;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.repository.RepositoryEntry;
import org.zinutils.exceptions.NotImplementedException;

public class Primitive implements RepositoryEntry, NamedType, NamedThing {
	private final InputPosition loc;
	private final SolidName name;
	private Predicate<Set<Type>> accepting;

	public Primitive(InputPosition loc, String name) {
		this.loc = loc;
		this.name = new SolidName(new PackageName(true), name);
	}

	@Override
	public InputPosition location() {
		return loc;
	}

	@Override
	public NameOfThing getName() {
		return name;
	}

	@Override
	public String signature() {
		return ((SolidName)this.name()).baseName();
	}

	@Override
	public int argCount() {
		return 0;
	}

	@Override
	public Type get(int pos) {
		throw new NotImplementedException();
	}

	@Override
	public NameOfThing name() {
		return getName();
	}
	
	@Override
	public void dumpTo(PrintWriter pw) {
		pw.print(signature());
	}
	
	@Override
	public String toString() {
		return "Primitive[" + name().uniqueName() + "]";
	}

	@Override
	public boolean incorporates(InputPosition pos, Type other) {
		if (other instanceof UnifiableType) {
			((UnifiableType)other).incorporatedBy(pos, this);
			return true;
		}
		if (other instanceof Primitive && ((Primitive)other).name().uniqueName().equals(name().uniqueName()))
			return true;
		if (this.name.uniqueName().equals("Any"))
			return true;
		if (this.name.uniqueName().equals("Entity"))
			return other instanceof StructDefn && ((StructDefn)other).type == FieldsType.ENTITY;
		if (this.name.uniqueName().equals("Contract") && (other instanceof ContractDecl || other instanceof HandlerImplements))
			return true;
		Set<Type> os = new HashSet<>();
		os.add(other);
		return willAcceptAll(os);
	}
	
	public void accept(Predicate<Set<Type>> fn) {
		accepting = fn;
	}

	public boolean willAcceptAll(Set<Type> ms) {
		return accepting != null && accepting.test(ms);
	}
}
