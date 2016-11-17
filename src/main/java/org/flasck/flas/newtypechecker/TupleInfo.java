package org.flasck.flas.newtypechecker;

import java.util.List;

import org.flasck.flas.blockForm.InputPosition;

public class TupleInfo extends TypeInfo {
	private final InputPosition location;
	public final List<TypeInfo> args;

	public TupleInfo(InputPosition location, List<TypeInfo> argtypes) {
		this.location = location;
		this.args = argtypes;
	}
	
	public InputPosition location() {
		return location;
	}

	@Override
	public String toString() {
		return "()"+args;
	}
}
