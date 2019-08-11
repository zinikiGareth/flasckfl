package org.flasck.flas.parsedForm;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Locatable;
import org.flasck.flas.commonBase.names.VarName;

public class StructField implements Locatable {
	public final InputPosition loc;
	public final InputPosition assOp;
	public final boolean accessor;
	public final TypeReference type;
	public final String name;
	public final Object init;
	private VarName myName;

	public StructField(InputPosition loc, boolean accessor, TypeReference type, String name) {
		this(loc, null, accessor, type, name, null);
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

	@Override
	public String toString() {
		if (type == null)
			return name + " (/" + loc.off + ")";
		else
			return type + " " + name + " (" + type.location().off + "/" + loc.off + ")";
	}

	public void fullName(VarName nameVar) {
		this.myName = nameVar;
	}
	
	public VarName name() {
		return myName;
	}
}
