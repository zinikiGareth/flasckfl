package org.flasck.flas.rewrittenForm;

import java.util.ArrayList;
import java.util.List;

public class RWTuplePattern {
	public final List<Object> args = new ArrayList<Object>();

	public RWTuplePattern(List<Object> arr) {
		for (Object o : arr)
			args.add(o);
	}
}
