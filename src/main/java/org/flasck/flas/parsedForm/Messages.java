package org.flasck.flas.parsedForm;

import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Expr;

public class Messages implements Expr {
	private final InputPosition location;
	public final List<Expr> exprs;

	public Messages(InputPosition location, List<Expr> exprs) {
		this.location = location;
		this.exprs = exprs;
	}

	@Override
	public InputPosition location() {
		return location;
	}
}
