package org.flasck.flas.parsedForm;

import java.io.Serializable;

@SuppressWarnings("serial")
public class StructField implements Serializable {
	public final TypeReference type;
	public final String name;
	public final Object init;

	public StructField(TypeReference type, String name) {
		this.type = type;
		this.name = name;
		this.init = null;
	}

	public StructField(TypeReference type, String name, Object init) {
		this.type = type;
		this.name = name;
		this.init = init;
	}
}
