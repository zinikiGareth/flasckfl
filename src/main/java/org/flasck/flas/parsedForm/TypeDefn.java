package org.flasck.flas.parsedForm;

import java.util.ArrayList;
import java.util.List;

public class TypeDefn {
	public final TypeReference defining;
	public final List<TypeReference> cases = new ArrayList<TypeReference>();

	public TypeDefn(TypeReference defining) {
		this.defining = defining;
	}

}
