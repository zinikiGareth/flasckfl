package org.flasck.flas.parsedForm;

import org.flasck.flas.blockForm.InputPosition;

public class D3Intro {
	public final InputPosition location;
	public final String name;
	public final Object expr;
	public final String var;

	public D3Intro(InputPosition location, String text, Object object, String var) {
		this.location = location;
		this.name = text;
		this.expr = object;
		this.var = var;
	}

	@Override
	public String toString() {
		return "D3[" + name +","+ expr +"," + var +"]";
	}
}
