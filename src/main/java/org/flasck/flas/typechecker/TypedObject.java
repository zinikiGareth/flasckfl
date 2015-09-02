package org.flasck.flas.typechecker;

public class TypedObject {
	public final Type type;
	public final Object expr;
	
	public TypedObject(Type type, Object expr) {
		this.type = type;
		this.expr = expr;
	}
}
