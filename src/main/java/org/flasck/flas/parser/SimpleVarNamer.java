package org.flasck.flas.parser;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.commonBase.names.SolidName;
import org.flasck.flas.commonBase.names.VarName;

public class SimpleVarNamer implements VarNamer {
	private final NameOfThing parent;

	public SimpleVarNamer(NameOfThing name) {
		this.parent = name;
	}

	@Override
	public VarName nameVar(InputPosition loc, String name) {
		return new VarName(loc, parent, name);
	}

	@Override
	public SolidName namePoly(InputPosition pos, String tok) {
		return new SolidName(parent, tok);
	}

}
