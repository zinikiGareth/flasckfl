package org.flasck.flas.types;

import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.NameOfThing;
import org.zinutils.exceptions.UtilException;

public class TypeWithNameAndPolys extends TypeWithName {
	protected final List<PolyVar> polys; // polymorphic arguments to REF, STRUCT, UNION, OBJECT or INSTANCE

	public TypeWithNameAndPolys(InputPosition kw, InputPosition location, NameOfThing type, List<PolyVar> polys) {
		super(kw, location, type);
		this.polys = polys;
	}

	public boolean hasPolys() {
		return polys != null && !polys.isEmpty();
	}
	
	public List<PolyVar> polys() {
		if (polys == null)
			throw new UtilException("Cannot obtain poly vars of " + name());
		return polys;
	}

	public PolyVar poly(int i) {
		if (polys == null)
			throw new UtilException("Cannot obtain poly vars of " + name());
		return polys.get(i);
	}

	protected void show(StringBuilder sb) {
		sb.append(name());
        if (polys != null && !polys.isEmpty())
        	sb.append(polys);
	}
}
