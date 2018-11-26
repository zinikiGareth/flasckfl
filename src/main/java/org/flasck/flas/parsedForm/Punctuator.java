package org.flasck.flas.parsedForm;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Expr;

public class Punctuator implements Expr {
	public final InputPosition location;
	public final String punc;

	public Punctuator(InputPosition location, String punc) {
		this.location = location;
		this.punc = punc;
	}
	
	@Override
	public InputPosition location() {
		return location;
	}

	@Override
	public String toString() {
		return punc;
	}
}
