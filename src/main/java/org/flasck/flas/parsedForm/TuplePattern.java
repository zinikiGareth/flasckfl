package org.flasck.flas.parsedForm;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Locatable;

public class TuplePattern implements Locatable {
	public final List<Object> args = new ArrayList<Object>();
	private final InputPosition loc;

	public TuplePattern(InputPosition loc, List<Object> arr) {
		this.loc = loc;
		for (Object o : arr)
			args.add(o);
	}

	@Override
	public InputPosition location() {
		return loc;
	}
}
