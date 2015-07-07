package org.flasck.flas.parser;

import org.flasck.flas.errors.ErrorResult;
import org.flasck.flas.parsedForm.PropertyDefn;
import org.flasck.flas.tokenizers.ExprToken;
import org.flasck.flas.tokenizers.Tokenizable;
import org.flasck.flas.tokenizers.ValidIdentifierToken;

public class PropertyParser implements TryParsing {

	@Override
	public Object tryParsing(Tokenizable line) {
		ValidIdentifierToken var = ValidIdentifierToken.from(line);
		if (var == null)
			return ErrorResult.oneMessage(line.realinfo(), "syntax error");
		
		ExprToken equals = ExprToken.from(line);
		if (equals == null || equals.type != ExprToken.SYMBOL || !equals.text.equals("="))
			return ErrorResult.oneMessage(line.realinfo(), "syntax error");

		Object expr = new Expression().tryParsing(line);
		if (expr == null)
			return ErrorResult.oneMessage(line, "syntax error");
		else if (expr instanceof ErrorResult)
			return expr;
		else
			return new PropertyDefn(var.location, var.text, expr);
	}


}
