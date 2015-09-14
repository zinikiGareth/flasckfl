package org.flasck.flas.parser;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.LocatedToken;
import org.flasck.flas.errors.ErrorResult;
import org.flasck.flas.parsedForm.Locatable;
import org.flasck.flas.parsedForm.MethodMessage;
import org.flasck.flas.tokenizers.MessageToken;
import org.flasck.flas.tokenizers.Tokenizable;

public class MethodMessageParser implements TryParsing {

	@Override
	public Object tryParsing(Tokenizable line) {
		MessageToken tok = null;
		List<Locatable> keys = new ArrayList<Locatable>();
		while (line.hasMore()) {
			// Odd tokens: first can be <- or ID, after that *must* be an identifier
			tok = MessageToken.from(line);
			if (tok == null)
				return ErrorResult.oneMessage(line, "unexpected symbol");
			else if (keys.isEmpty() && tok.type == MessageToken.ARROW)
				break;
			else if (tok.type == MessageToken.IDENTIFIER)
				keys.add(new LocatedToken(tok.location, tok.text));
			else
				return ErrorResult.oneMessage(line, "syntax error 1");

			// Even tokens: may be "." or "<-"
			tok = MessageToken.from(line);
			if (tok == null)
				return ErrorResult.oneMessage(line, "unexpected symbol");
			else if (tok.type == MessageToken.ARROW)
				break;
			else if (tok.type == MessageToken.DOT)
				;
			else
				return ErrorResult.oneMessage(line, "syntax error 2");
		}
		if (tok == null || tok.type != MessageToken.ARROW)
			return ErrorResult.oneMessage(line, "");
		Object expr = new Expression().tryParsing(line);
		if (expr == null)
			return ErrorResult.oneMessage(line, "syntax error");
		else if (expr instanceof ErrorResult)
			return expr;
		if (line.hasMore())
			return ErrorResult.oneMessage(line, "tokens at end of line");
		else
			return new MethodMessage(keys.isEmpty()?null:keys, expr);
	}

}
