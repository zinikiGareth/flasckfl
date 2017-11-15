package org.flasck.flas.rewrittenForm;

import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.types.FunctionType;

public class RWObjectMethod {
	public final FunctionName name;
	public final FunctionType type;
	final RWMethodDefinition defn;

	public RWObjectMethod(FunctionType type, FunctionName name) {
		this.name = name;
		this.type = type;
		this.defn = null;
	}

	public RWObjectMethod(RWMethodDefinition rw, FunctionType type) {
		this.name = rw.name();
		this.type = type;
		this.defn = rw;
	}
}
