package org.flasck.flas.parsedForm;

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
