package org.flasck.flas.parser;

import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.errors.ErrorResult;
import org.flasck.flas.parsedForm.D3PatternBlock;
import org.flasck.flas.tokenizers.ExprToken;
import org.flasck.flas.tokenizers.KeywordToken;
import org.flasck.flas.tokenizers.Tokenizable;

public class D3PatternLineParser implements TryParsing {

	@Override
	public Object tryParsing(Tokenizable line) {
		if (!line.hasMore())
			return null;
		KeywordToken kw = KeywordToken.from(line);
		if (kw == null)
			return null; // in the "nothing doing" sense

		if (!kw.text.equals("pattern"))
			return ErrorResult.oneMessage(kw.location, "'pattern' expected");
		
		ExprToken sl = ExprToken.from(line);
		if (sl == null)
			return ErrorResult.oneMessage(line.realinfo(), "pattern string required");
		
		if (sl.type != ExprToken.STRING)
			return ErrorResult.oneMessage(sl.location, "pattern must be a literal string");
		
		return new D3PatternBlock(kw.location, new StringLiteral(sl.location, sl.text));
	}

}
