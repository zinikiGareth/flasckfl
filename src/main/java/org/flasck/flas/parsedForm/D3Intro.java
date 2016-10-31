package org.flasck.flas.parsedForm;

import org.flasck.flas.blockForm.InputPosition;

public class D3Intro {
	public final InputPosition kw;
	public final InputPosition nameLoc;
	public final String name;
	public final Object expr;
	public final InputPosition varLoc;
	public final String iterVar;

	public D3Intro(InputPosition kw, InputPosition location, String name, Object object, InputPosition varLoc, String iterVar) {
		this.kw = kw;
		this.nameLoc = location;
		this.name = name;
		this.expr = object;
		this.varLoc = varLoc;
		this.iterVar = iterVar;
	}

	@Override
	public String toString() {
		return "D3[" + name +","+ expr +"," + iterVar +"]";
	}
}
