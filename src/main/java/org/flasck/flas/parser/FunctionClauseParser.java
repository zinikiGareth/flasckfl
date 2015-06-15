package org.flasck.flas.parser;

import org.flasck.flas.errors.ErrorResult;
import org.flasck.flas.parsedForm.FunctionClause;
import org.flasck.flas.tokenizers.ExprToken;
import org.flasck.flas.tokenizers.Tokenizable;

public class FunctionClauseParser implements TryParsing {

	@Override
	public Object tryParsing(Tokenizable line) {
		Expression ep = new Expression();
		ExprToken tok = ExprToken.from(line);
		if (tok == null)
			return null;
		Object guard = null;
		if (tok.text.equals("if")) {
			tok = ExprToken.from(line);
			if (tok == null || !tok.text.equals("("))
				return ErrorResult.oneMessage(line, "expected (");
			guard = ep.tryParsing(line);
			if (guard == null || guard instanceof ErrorResult)
				return guard;
			tok = ExprToken.from(line);
			if (tok == null || !tok.text.equals(")"))
				return ErrorResult.oneMessage(line, "expected )");
			tok = null;
		} else if (tok.text.equals("else"))
			tok = null;
		if (tok == null)
			tok = ExprToken.from(line);
		if (tok == null || !tok.text.equals("="))
			return ErrorResult.oneMessage(line, "expected =");
		Object expr = ep.tryParsing(line);
		if (expr == null || expr instanceof ErrorResult)
			return expr;
		return new FunctionClause(guard, expr);
	}

}
