package org.flasck.flas.newtypechecker;

import java.util.Comparator;

public class TypeInfoComparator implements Comparator<TypeInfo> {

	@Override
	public int compare(TypeInfo o1, TypeInfo o2) {
		// hack for now
		return o1.toString().compareTo(o2.toString());
	}

}
