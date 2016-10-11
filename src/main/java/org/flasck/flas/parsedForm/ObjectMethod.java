package org.flasck.flas.parsedForm;

import java.io.Serializable;

@SuppressWarnings("serial")
public class ObjectMethod implements Serializable {
	public final String name;
	public final TypeReference type;

	public ObjectMethod(TypeReference type, String name) {
		this.name = name;
		this.type = type;
	}
}
