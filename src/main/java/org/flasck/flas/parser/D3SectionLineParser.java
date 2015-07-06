package org.flasck.flas.parser;

import org.flasck.flas.errors.ErrorResult;
import org.flasck.flas.parsedForm.D3Section;
import org.flasck.flas.tokenizers.KeywordToken;
import org.flasck.flas.tokenizers.Tokenizable;

public class D3SectionLineParser implements TryParsing {

	@Override
	public Object tryParsing(Tokenizable line) {
		if (!line.hasMore())
			return null;
		KeywordToken kw = KeywordToken.from(line);
		if (kw == null)
			return null; // in the "nothing doing" sense

		if (kw.text.equals("enter")) {
			return new D3Section("enter");
		} else
			return ErrorResult.oneMessage(kw.location, kw.text + " expected");
	}

}
