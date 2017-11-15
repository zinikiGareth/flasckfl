package org.flasck.flas.rewrittenForm;

import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.types.FunctionType;

public class RWObjectMethod {
	public final FunctionName name;
	public final FunctionType type;

	public RWObjectMethod(FunctionType type, FunctionName name) {
		this.name = name;
		this.type = type;
	}
}
