package org.flasck.flas.rewrittenForm;

import java.util.ArrayList;
import java.util.List;

public class TuplePattern {
	public final List<Object> args = new ArrayList<Object>();

	public TuplePattern(List<Object> arr) {
		for (Object o : arr)
			args.add(o);
	}
}
