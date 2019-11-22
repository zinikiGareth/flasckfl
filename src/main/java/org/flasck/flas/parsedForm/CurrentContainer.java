package org.flasck.flas.parsedForm;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Expr;

public class CurrentContainer implements Expr {
	private final InputPosition pos;

	public CurrentContainer(InputPosition pos) {
		this.pos = pos;
	}

	@Override
	public InputPosition location() {
		return pos;
	}

}
