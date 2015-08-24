package org.flasck.flas.typechecker;

public class VariableFactory {
	private int nextVar = 1;
	
	public TypeVar next() {
		return new TypeVar(null, nextVar++);
	}
}
