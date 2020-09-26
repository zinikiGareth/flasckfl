package org.flasck.flas.parsedForm;

import java.io.PrintWriter;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Locatable;
import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.commonBase.names.SolidName;
import org.flasck.flas.repository.RepositoryEntry;
import org.flasck.flas.tc3.NamedType;
import org.flasck.flas.tc3.Type;
import org.zinutils.exceptions.NotImplementedException;

public class PolyType implements RepositoryEntry, Locatable, NamedType, Comparable<PolyType> {
	private final InputPosition location;
	private final SolidName fullName;
	private final String shortName;

	public PolyType(InputPosition location, SolidName solidName) {
		this.location = location;
		this.fullName = solidName;
		this.shortName = solidName.baseName();
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof PolyType))
			return false;
		PolyType other = (PolyType) obj;
		return fullName.uniqueName().equals(other.fullName.uniqueName());
	}
	
	@Override
	public int hashCode() {
		return shortName.hashCode();
	}
	
	public String shortName() {
		return shortName;
	}
	
	public NameOfThing name() {
		return fullName;
	}

	public InputPosition location() {
		return location;
	}

	@Override
	public String toString() {
		return fullName.uniqueName();
	}

	@Override
	public String signature() {
		return shortName;
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
	public boolean incorporates(InputPosition pos, Type other) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public int compareTo(PolyType o) {
		return this.shortName.compareTo(o.shortName);
	}

	@Override
	public void dumpTo(PrintWriter pw) {
		pw.println("PolyType[" + this.fullName.uniqueName() + "]");
	}
}
