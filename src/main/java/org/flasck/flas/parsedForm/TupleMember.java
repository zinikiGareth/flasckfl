package org.flasck.flas.parsedForm;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Locatable;
import org.flasck.flas.commonBase.names.FunctionName;

public class TupleMember implements Locatable {
	private InputPosition location;
	public final TupleAssignment ta;
	public final int which;

	public TupleMember(InputPosition location, TupleAssignment ta, int which) {
		this.location = location;
		this.ta = ta;
		this.which = which;
	}

	@Override
	public InputPosition location() {
		return location;
	}

	public FunctionName name() {
		return ta.leadName();
	}

}
