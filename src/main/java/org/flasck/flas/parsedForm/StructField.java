package org.flasck.flas.parsedForm;

public class StructField {
	public final Object type;
	public final String name;
	public final Object init;

	public StructField(Object type, String kw) {
		this.type = type;
		this.name = kw;
		this.init = null;
	}

	public StructField(Object type, String kw, Object init) {
		this.type = type;
		name = kw;
		this.init = init;
	}
}
