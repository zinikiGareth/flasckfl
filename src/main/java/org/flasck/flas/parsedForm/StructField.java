package org.flasck.flas.parsedForm;

import java.io.Serializable;

import org.flasck.flas.typechecker.Type;

@SuppressWarnings("serial")
public class StructField implements Serializable {
	public final Type type;
	public final String name;
	public final Object init;

	public StructField(Type type, String name) {
		this.type = type;
		this.name = name;
		this.init = null;
	}

	public StructField(Type type, String name, Object init) {
		this.type = type;
		this.name = name;
		this.init = init;
	}
}
