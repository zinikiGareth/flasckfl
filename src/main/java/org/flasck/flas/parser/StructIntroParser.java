package org.flasck.flas.parser;

import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.tokenizers.KeywordToken;
import org.flasck.flas.tokenizers.Tokenizable;
import org.flasck.flas.tokenizers.TypeNameToken;

public class StructIntroParser implements TryParsing {

	@Override
	public Object tryParsing(Tokenizable line) {
		String kw = KeywordToken.from(line);
		if (!"struct".equals(kw))
			return null;
		String tn = TypeNameToken.from(line);
		if (tn == null)
			return null; // invalid type name
		
		StructDefn ret = new StructDefn(tn);
		while (line.hasMore()) {
			String ta = TypeNameToken.from(line);
			if (ta == null)
				return null; // invalid type argument
			else
				ret.add(ta);
		}
		return ret;
	}

}
