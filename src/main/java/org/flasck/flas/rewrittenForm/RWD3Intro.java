package org.flasck.flas.rewrittenForm;

import org.flasck.flas.blockForm.InputPosition;

public class RWD3Intro {
	public final InputPosition location;
	public final String name;
	public final Object expr;
	public final String var;

	public RWD3Intro(InputPosition location, String text, Object object, String var) {
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
