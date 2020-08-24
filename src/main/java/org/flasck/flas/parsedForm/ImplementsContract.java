package org.flasck.flas.parsedForm;

import java.io.PrintWriter;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.CSName;
import org.flasck.flas.repository.RepositoryEntry;
import org.flasck.flas.tc3.NamedType;
import org.flasck.flas.tc3.Type;
import org.zinutils.exceptions.CantHappenException;
import org.zinutils.exceptions.NotImplementedException;

public class ImplementsContract extends Implements implements RepositoryEntry, NamedType, StateHolder {
	public ImplementsContract(InputPosition kw, InputPosition location, NamedType parent, TypeReference type, CSName csn) {
		super(kw, location, parent, type, csn);
	}
	
	@Override
	public void dumpTo(PrintWriter pw) {
		pw.println(toString());
	}

	@Override
	public String toString() {
		return super.toString();
	}

	@Override
	public String signature() {
		return name().uniqueName();
	}

	@Override
	public int argCount() {
		return 0;
	}

	@Override
	public Type get(int pos) {
		if (pos != 0)
			throw new CantHappenException("pos = " + pos);
		return this;
	}

	@Override
	public boolean incorporates(InputPosition pos, Type other) {
		return this == other || other == this.actualType();
	}

	@Override
	public StateDefinition state() {
		throw new NotImplementedException();
	}
}
