package org.flasck.flas.parsedForm;

public class ObjectAccessor {
	private final FunctionDefinition fn;

	public ObjectAccessor(FunctionDefinition fn) {
		this.fn = fn;
	}
	
	public FunctionDefinition function() {
		return fn;
	}

	@Override
	public String toString() {
		return "Some ObjectAccessor";
	}
}
