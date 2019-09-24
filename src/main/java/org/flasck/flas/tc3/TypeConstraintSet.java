package org.flasck.flas.tc3;

import org.flasck.flas.parsedForm.PolyType;
import org.zinutils.exceptions.NotImplementedException;

public class TypeConstraintSet implements UnifiableType {
	private PolyType t = null;
	
	@Override
	public Type resolve() {
		t = new PolyType(null, "A"); 
		return t;
	}
	
	@Override
	public String signature() {
		if (t == null)
			throw new NotImplementedException("Has not been resolved");
		return t.name();
	}

	@Override
	public int argCount() {
		throw new NotImplementedException();
	}

	@Override
	public Type get(int pos) {
		throw new NotImplementedException();
	}

}
