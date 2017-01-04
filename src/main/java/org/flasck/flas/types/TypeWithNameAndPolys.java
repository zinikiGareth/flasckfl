package org.flasck.flas.types;

import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.NameOfThing;
import org.zinutils.exceptions.UtilException;

public class TypeWithNameAndPolys extends TypeWithName {
	protected final List<Type> polys; // polymorphic arguments to REF, STRUCT, UNION, OBJECT or INSTANCE

	public TypeWithNameAndPolys(InputPosition kw, InputPosition location, NameOfThing type, List<Type> polys) {
		super(kw, location, type);
		this.polys = polys;
		if (polys != null)
			for (Type t : polys)
				if (!(t instanceof PolyVar))
					throw new UtilException("All arguments to type defn must be poly vars");
	}

	public boolean hasPolys() {
		return polys != null && !polys.isEmpty();
	}
	
	public List<Type> polys() {
		if (polys == null)
			throw new UtilException("Cannot obtain poly vars of " + name());
		return polys;
	}

	public Type poly(int i) {
		if (polys == null)
			throw new UtilException("Cannot obtain poly vars of " + name());
		return polys.get(i);
	}

	protected void show(StringBuilder sb) {
		sb.append(name);
        if (polys != null && !polys.isEmpty())
        	sb.append(polys);
	}
}
