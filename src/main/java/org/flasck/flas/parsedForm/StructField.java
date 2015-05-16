package org.flasck.flas.parsedForm;

public class StructField {
	public final Object type;
	public final String name;

	public StructField(Object type, String kw) {
		this.type = type;
		this.name = kw;
	}
}
