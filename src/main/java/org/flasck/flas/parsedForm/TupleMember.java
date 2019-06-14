package org.flasck.flas.parsedForm;

import java.io.PrintWriter;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Locatable;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.repository.RepositoryEntry;

public class TupleMember implements Locatable, RepositoryEntry {
	private InputPosition location;
	public final TupleAssignment ta;
	public final int which;
	private final FunctionName myName;

	public TupleMember(InputPosition location, TupleAssignment ta, int which, FunctionName myName) {
		this.location = location;
		this.ta = ta;
		this.which = which;
		this.myName = myName;
	}

	@Override
	public InputPosition location() {
		return location;
	}

	public FunctionName exprFnName() {
		return ta.exprFnName();
	}

	public FunctionName name() {
		return myName;
	}

	@Override
	public void dumpTo(PrintWriter pw) {
		throw new org.zinutils.exceptions.NotImplementedException();
	}
}
