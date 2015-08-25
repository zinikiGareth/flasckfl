package org.flasck.flas.parsedForm;

import org.flasck.flas.typechecker.Type;

public class ObjectMethod {
	public final String name;
	public final Type type;

	public ObjectMethod(Type type, String name) {
		this.name = name;
		this.type = type;
	}
}
