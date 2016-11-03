package org.flasck.flas.newtypechecker;

public class TypeVar extends TypeInfo {

	private int idx;

	public TypeVar(int idx) {
		this.idx = idx;
	}
	
	@Override
	public String toString() {
		return "tv_" + idx;
	}
}
