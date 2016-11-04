package org.flasck.flas.newtypechecker;

import org.flasck.flas.typechecker.Type;

public class InstanceType extends TypeInfo {
	private final Type type;

	public InstanceType(Type type) {
		this.type = type;
	}

	@Override
	public String toString() {
		return "Instance[" + type + "]";
	}
}
