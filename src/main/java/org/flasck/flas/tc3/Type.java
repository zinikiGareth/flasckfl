package org.flasck.flas.tc3;

import org.flasck.flas.blockForm.InputPosition;

public interface Type {
	String signature();
	int argCount();
	Type get(int pos);
	boolean incorporates(InputPosition pos, Type other);
}
