package org.flasck.flas.newtypechecker;

import java.util.Comparator;

public class ConstraintComparator implements Comparator<Constraint> {

	@Override
	public int compare(Constraint o1, Constraint o2) {
		// a hack for now 
		return o1.toString().compareTo(o2.toString());
	}

}
