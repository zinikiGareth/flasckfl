package org.flasck.flas.parsedForm;


import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Locatable;

public class StructField implements Locatable {
	public final InputPosition loc;
	public final InputPosition assOp;
	public final boolean accessor;
	public final TypeReference type;
	public final String name;
	public final Object init;

	public StructField(InputPosition loc, boolean accessor, TypeReference type, String name) {
		this.loc = loc;
		this.assOp = null;
		this.accessor = accessor;
		this.type = type;
		this.name = name;
		this.init = null;
	}

	public StructField(InputPosition loc, InputPosition assOp, boolean accessor, TypeReference type, String name, Object init) {
		this.loc = loc;
		this.assOp = assOp;
		this.accessor = accessor;
		this.type = type;
		this.name = name;
		this.init = init;
	}

	@Override
	public InputPosition location() {
		return loc;
	}
}
