package org.flasck.flas.parsedForm;

import org.flasck.flas.commonBase.names.FunctionName;

public class ObjectAccessor {
	private final FunctionDefinition fn;

	public ObjectAccessor(FunctionDefinition fn) {
		this.fn = fn;
	}
	
	public FunctionName name() {
		return fn.name();
	}
	
	public FunctionDefinition function() {
		return fn;
	}

	@Override
	public String toString() {
		return "Some ObjectAccessor";
	}
}
