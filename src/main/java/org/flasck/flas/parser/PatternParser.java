package org.flasck.flas.parser;

import org.flasck.flas.tokenizers.ExprToken;
import org.flasck.flas.tokenizers.Tokenizable;

public class PatternParser implements TryParsing {

	@Override
	public Object tryParsing(Tokenizable line) {
		ExprToken tok = ExprToken.from(line);
		if (tok == null)
			return null;
		
		if (tok.type == ExprToken.IDENTIFIER || tok.type == ExprToken.NUMBER)
			return tok.text;
		else if (tok.type == ExprToken.PUNC && tok.text.equals("(")) {
			
		}
			
//		if (tok == null || !line.hasMore())
			return null;

//		return tok;
	}

}
