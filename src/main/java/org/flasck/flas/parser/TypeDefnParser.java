package org.flasck.flas.parser;

import org.flasck.flas.parsedForm.TypeDefn;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.tokenizers.KeywordToken;
import org.flasck.flas.tokenizers.PeekToken;
import org.flasck.flas.tokenizers.Tokenizable;
import org.flasck.flas.tokenizers.TypeNameToken;

public class TypeDefnParser implements TryParsing {

	@Override
	public Object tryParsing(Tokenizable line) {
		String kw = KeywordToken.from(line);
		if (!"type".equals(kw))
			return null;
		String tn = TypeNameToken.from(line);
		if (tn == null)
			return null; // invalid type name

		TypeReference defining = new TypeReference(tn);
		while (line.hasMore() && !PeekToken.is(line, "=")) {
			String ta = TypeNameToken.from(line);
			if (ta == null)
				return null; // invalid type argument
			defining.args.add(new TypeReference(ta));
		}
		if (!PeekToken.accept(line, "="))
			return null;
		TypeDefn ret = new TypeDefn(defining);
		while (line.hasMore()) {
			Object tr = new TypeExprParser().tryOneExpr(line);
			if (tr == null)
				return null; // invalid type argument
			else
				ret.cases.add((TypeReference) tr);
			if (line.hasMore())
				PeekToken.accept(line, "|");
		}
		return ret;
	}

}
