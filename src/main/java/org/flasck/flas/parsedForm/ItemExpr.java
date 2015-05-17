package org.flasck.flas.parsedForm;

import org.flasck.flas.tokenizers.ExprToken;

public class ItemExpr {
	public final ExprToken tok;

	public ItemExpr(ExprToken tok) {
		this.tok = tok;
	}

	@Override
	public String toString() {
		return tok.text;
	}
}
