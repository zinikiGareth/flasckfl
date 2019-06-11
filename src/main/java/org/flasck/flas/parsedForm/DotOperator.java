package org.flasck.flas.parsedForm;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Expr;

public class DotOperator implements Expr {
	public final InputPosition location;

	public DotOperator(InputPosition location) {
		this.location = location;
	}
	
	@Override
	public InputPosition location() {
		return location;
	}

	@Override
	public String toString() {
		return ".";
	}
}
