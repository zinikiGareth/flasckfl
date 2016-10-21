package org.flasck.flas.parsedForm;

public class ObjectMethod {
	public final String name;
	public final TypeReference type;

	public ObjectMethod(TypeReference type, String name) {
		this.name = name;
		this.type = type;
	}
}
