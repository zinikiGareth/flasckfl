package org.flasck.flas.parsedForm;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Expr;

public class MakeSend implements Expr {
	private final InputPosition pos;
	public final int nargs;

	public MakeSend(InputPosition pos, int nargs) {
		this.pos = pos;
		this.nargs = nargs;
	}

	@Override
	public InputPosition location() {
		return pos;
	}
}
