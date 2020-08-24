package org.flasck.flas.parsedForm;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.tc3.NamedType;
import org.zinutils.exceptions.NotImplementedException;

public class CurrentContainer implements Expr {
	private final InputPosition pos;
	public final NamedType type;

	public CurrentContainer(InputPosition pos, NamedType type) {
		this.pos = pos;
		if (type == null)
			throw new NotImplementedException("Type cannot be null");
		System.out.println("Creating container with type " + type);
		this.type = type;
	}

	@Override
	public InputPosition location() {
		return pos;
	}

}
