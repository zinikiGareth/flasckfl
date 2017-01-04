package org.flasck.flas.rewrittenForm;

import org.flasck.flas.types.FunctionType;

public class RWObjectMethod {
	public final String name;
	public final FunctionType type;

	public RWObjectMethod(FunctionType type, String name) {
		this.name = name;
		this.type = type;
	}
}
