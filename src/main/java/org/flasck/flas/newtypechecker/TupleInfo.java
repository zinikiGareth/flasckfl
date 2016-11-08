package org.flasck.flas.newtypechecker;

import java.util.List;

public class TupleInfo extends TypeInfo {
	public final List<TypeInfo> args;

	public TupleInfo(List<TypeInfo> argtypes) {
		this.args = argtypes;
	}

	@Override
	public String toString() {
		return "()"+args;
	}
}
