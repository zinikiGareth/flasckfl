package org.flasck.flas.parsedForm;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.tc3.NamedType;

public class CurrentContainer implements Expr {
	private final InputPosition pos;
	public final NamedType type;

	public CurrentContainer(InputPosition pos, NamedType type) {
		this.pos = pos;
		this.type = type;
	}

	@Override
	public InputPosition location() {
		return pos;
	}

}
