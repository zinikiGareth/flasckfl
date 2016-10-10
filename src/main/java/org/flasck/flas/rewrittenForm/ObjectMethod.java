package org.flasck.flas.rewrittenForm;

import java.io.Serializable;

import org.flasck.flas.typechecker.Type;

@SuppressWarnings("serial")
public class ObjectMethod implements Serializable {
	public final String name;
	public final Type type;

	public ObjectMethod(Type type, String name) {
		this.name = name;
		this.type = type;
	}
}
