package org.flasck.flas.tc3;

import java.io.PrintWriter;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.commonBase.names.NamedThing;
import org.flasck.flas.commonBase.names.SolidName;
import org.flasck.flas.repository.RepositoryEntry;
import org.flasck.flas.types.TypeWithName;
import org.zinutils.exceptions.NotImplementedException;

public class Primitive extends TypeWithName implements RepositoryEntry, Type, NamedThing {
	public Primitive(InputPosition loc, String name) {
		super(null, loc, new SolidName(null, name));
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
	public boolean incorporates(Type other) {
		return other instanceof Primitive && ((Primitive)other).name().uniqueName().equals(name().uniqueName());
	}
}
