package org.flasck.flas.types;

import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.commonBase.names.NamedThing;
import org.zinutils.collections.CollectionUtils;

public class TypeWithName extends Type implements NamedThing {
	protected final NameOfThing typeName;

	public TypeWithName(InputPosition kw, InputPosition location, NameOfThing type) {
		super(location);
		this.typeName = type;
	}

	public String name() {
		return typeName.uniqueName();
	}
	
	public Type instance(InputPosition loc, Type... with) {
		return new InstanceType(loc, this, CollectionUtils.listOf(with));
	}

	public Type instance(InputPosition loc, List<Type> with) {
		return new InstanceType(loc, this, with);
	}

	@Override
	public NameOfThing getName() {
		return typeName;
	}

	public NameOfThing getTypeName() {
		return typeName;
	}
	
	protected void show(StringBuilder sb) {
		sb.append(name());
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == null)
			return false;
		if (!(obj instanceof TypeWithName))
			return false;
		TypeWithName other = (TypeWithName)obj;
		return typeName.equals(other.typeName);
	}

}