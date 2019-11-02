package org.flasck.flas.parsedForm;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Locatable;
import org.flasck.flas.tc3.Type;
import org.zinutils.exceptions.NotImplementedException;

public class PolyType implements Locatable, Type, Comparable<PolyType> {
	private InputPosition location;
	private String name;

	public PolyType(InputPosition location, String name) {
		this.location = location;
		this.name = name;
	}

	public String name() {
		return name;
	}

	public InputPosition location() {
		return location;
	}

	@Override
	public String toString() {
		return name;
	}

	@Override
	public String signature() {
		return name;
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
		return this.name.compareTo(o.name);
	}
}
