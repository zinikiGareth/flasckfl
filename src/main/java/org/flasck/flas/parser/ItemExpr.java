package org.flasck.flas.parser;

import org.flasck.flas.commonBase.NumericLiteral;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.parsedForm.UnresolvedOperator;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.tokenizers.ExprToken;
import org.zinutils.exceptions.UtilException;

public class ItemExpr {
	public static Object from(ExprToken tok) {
		if (tok.type == ExprToken.NUMBER)
			return new NumericLiteral(tok.location, tok.text, -1);
		else if (tok.type == ExprToken.STRING)
			return new StringLiteral(tok.location, tok.text);
		else if (tok.type == ExprToken.IDENTIFIER)
			return new UnresolvedVar(tok.location, tok.text);
		else if (tok.type == ExprToken.SYMBOL || tok.type == ExprToken.PUNC)
			return new UnresolvedOperator(tok.location, tok.text);
		else
			throw new UtilException("Case not handled");
	}
}
