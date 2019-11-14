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
	private InputPosition location;
	private String shortName;
	private NameOfThing fullName;

	public PolyType(InputPosition location, String name) {
		this.location = location;
		this.shortName = name;
	}

	public void containedIn(NameOfThing container) {
		this.fullName = new SolidName(container, shortName);
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
		return shortName;
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
	public boolean incorporates(Type other) {
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
