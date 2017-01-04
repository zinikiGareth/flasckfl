package org.flasck.flas.types;

import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Locatable;
import org.flasck.flas.commonBase.NameOfThing;
import org.zinutils.collections.CollectionUtils;
import org.zinutils.exceptions.NotImplementedException;
import org.zinutils.exceptions.UtilException;

public abstract class Type implements Locatable {
	public final InputPosition kw;
	private final InputPosition location;

	protected Type(InputPosition location) {
		this.kw = null;
		if (location == null)
			throw new UtilException("Type without input location 3");
		this.location = location;
	}

	@Override
	public InputPosition location() {
		return location;
	}

	public String name() { throw new NotImplementedException(); }
	public NameOfThing getTypeName() { throw new NotImplementedException(); }

	public boolean hasPolys() { return false; }
	public List<Type> polys() { throw new NotImplementedException(); }
	public Type poly(int i) { throw new NotImplementedException(); }

	public static FunctionType function(InputPosition loc, Type... args) {
		return new FunctionType(loc, CollectionUtils.listOf(args));
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		show(sb);
		return sb.toString();
	}

	protected abstract void show(StringBuilder sb);

	// This should be overriden a lot more
	@Override
	public boolean equals(Object obj) {
		if (obj == null)
			return false;
		if (!(obj instanceof Type))
			return false;
		return true;
	}
}
