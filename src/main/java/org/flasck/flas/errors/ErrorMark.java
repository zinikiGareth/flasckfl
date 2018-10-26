package org.flasck.flas.errors;

import java.util.Set;
import java.util.TreeSet;

public class ErrorMark {
	private Set<FLASError> have = new TreeSet<>();

	public ErrorMark(ErrorResult errs) {
		if (errs == null)
			return;
		for (FLASError e : errs)
			have.add(e);
	}

	public boolean contains(FLASError e) {
		return have.contains(e);
	}
}
