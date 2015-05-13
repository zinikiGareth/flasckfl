package org.flasck.flas.parser;

import org.flasck.flas.parsedForm.ItemExpr;
import org.flasck.flas.tokenizers.ExprToken;
import org.flasck.flas.tokenizers.Tokenizable;

public class Expression implements TryParsing {

	@Override
	public Object tryParsing(Tokenizable line) {
		ExprToken s = ExprToken.from(line);
		if (s.type == ExprToken.NUMBER || s.type == ExprToken.IDENTIFIER)
			return new ItemExpr(s);
		System.out.println("Need to handle apply");
		return null;
	}
}
