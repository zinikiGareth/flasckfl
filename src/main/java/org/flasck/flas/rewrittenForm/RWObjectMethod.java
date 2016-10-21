package org.flasck.flas.rewrittenForm;

import org.flasck.flas.typechecker.Type;

public class RWObjectMethod {
	public final String name;
	public final Type type;

	public RWObjectMethod(Type type, String name) {
		this.name = name;
		this.type = type;
	}
}
