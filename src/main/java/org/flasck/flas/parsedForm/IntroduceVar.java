package org.flasck.flas.parsedForm;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.repository.RepositoryEntry;

public class IntroduceVar implements Expr {
	public final InputPosition location;
	public final String var;
	private RepositoryEntry definition;

	public IntroduceVar(InputPosition location, String var) {
		this.location = location;
		this.var = var;
	}

	@Override
	public InputPosition location() {
		return location;
	}

	@Override
	public String toString() {
		return "_" + var;
	}
}
