package org.flasck.flas.rewrittenForm;

import java.io.Serializable;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.parsedForm.Locatable;
import org.flasck.flas.typechecker.Type;

@SuppressWarnings("serial")
public class RWStructField implements Locatable, Serializable {
	public final InputPosition loc;
	public final boolean accessor;
	public final Type type;
	public final String name;
	public final Object init;

	public RWStructField(InputPosition loc, boolean accessor, Type type, String name) {
		this.loc = loc;
		this.accessor = accessor;
		this.type = type;
		this.name = name;
		this.init = null;
	}

	public RWStructField(InputPosition loc, boolean accessor, Type type, String name, Object init) {
		this.loc = loc;
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
