package org.flasck.flas.typechecker;

import org.flasck.flas.types.Type;

public class TypedObject {
	public final Type type;
	public final Object expr;
	
	public TypedObject(Type type, Object expr) {
		this.type = type;
		this.expr = expr;
	}
}
