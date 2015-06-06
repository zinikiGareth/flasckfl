package org.flasck.flas.parsedForm;

import org.flasck.flas.tokenizers.ExprToken;

public class ItemExpr {
	public final ExprToken tok;

	public ItemExpr(ExprToken tok) {
		this.tok = tok;
	}
	
	public static ItemExpr id(String id) {
		return new ItemExpr(new ExprToken(ExprToken.IDENTIFIER, id));
	}

	public static ItemExpr str(String str) {
		return new ItemExpr(new ExprToken(ExprToken.STRING, str));
	}

	public static Object punc(String ch) {
		return new ItemExpr(new ExprToken(ExprToken.PUNC, ch));
	}

	@Override
	public String toString() {
		if (tok.type == ExprToken.STRING)
			return "'" + tok.text + "'";
		return tok.text;
	}
}
