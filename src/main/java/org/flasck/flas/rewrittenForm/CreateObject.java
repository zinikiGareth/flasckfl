package org.flasck.flas.rewrittenForm;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Locatable;
import org.flasck.flas.commonBase.names.SolidName;

public class CreateObject implements Locatable {
	private final InputPosition loc;
	public final SolidName name;
	public final Object expr;

	public CreateObject(InputPosition loc, SolidName name, Object expr) {
		this.loc = loc;
		this.name = name;
		this.expr = expr;
	}

	@Override
	public InputPosition location() {
		return loc;
	}

}
