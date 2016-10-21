package org.flasck.flas.rewrittenForm;

import org.flasck.flas.blockForm.InputPosition;

public class FunctionLiteral {
	public final InputPosition location;
	public final String name;

	public FunctionLiteral(InputPosition location, String text) {
		this.location = location;
		this.name = text;
	}
	
	@Override
	public String toString() {
		return name + "()";
	}

}
