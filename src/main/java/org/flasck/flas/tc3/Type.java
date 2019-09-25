package org.flasck.flas.tc3;

public interface Type {
	String signature();
	int argCount();
	Type get(int pos);
	boolean incorporates(Type other);
}
