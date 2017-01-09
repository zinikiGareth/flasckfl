package org.flasck.flas.flim;

public class BuiltinOperation {
	public final String opName;

	private BuiltinOperation(String which) {
		this.opName = which;
	}

	public static final BuiltinOperation FIELD = new BuiltinOperation("field");
	public static final BuiltinOperation TUPLE = new BuiltinOperation("tuple");
}
