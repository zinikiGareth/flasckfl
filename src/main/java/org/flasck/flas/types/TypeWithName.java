package org.flasck.flas.types;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.NameOfThing;

public class TypeWithName extends Type {
	protected final String name;
	protected final NameOfThing typeName;

	public TypeWithName(InputPosition kw, InputPosition location, NameOfThing type) {
		super(location);
		this.name = type.uniqueName();
		this.typeName = type;
	}

	public String name() {
		return name;
	}
	
	public NameOfThing getTypeName() {
		return typeName;
	}
	
	protected void show(StringBuilder sb) {
		sb.append(name);
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == null)
			return false;
		if (!(obj instanceof TypeWithName))
			return false;
		TypeWithName other = (TypeWithName)obj;
		// TODO: for completeness, we should check any polymorphic args
		if (name != null)
			return name.equals(other.name);
		else
			return true;
	}

}