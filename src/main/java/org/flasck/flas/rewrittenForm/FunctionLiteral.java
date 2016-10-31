package org.flasck.flas.rewrittenForm;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Locatable;

public class FunctionLiteral implements Locatable {
	public final InputPosition location;
	public final String name;

	public FunctionLiteral(InputPosition location, String text) {
		this.location = location;
		this.name = text;
	}
	
	@Override
	public InputPosition location() {
		return location;
	}
	
	@Override
	public String toString() {
		return name + "()";
	}

}
