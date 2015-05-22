package org.flasck.flas.parser;

import org.flasck.flas.ErrorResult;
import org.flasck.flas.parsedForm.CardDefiniton;
import org.flasck.flas.parsedForm.ContractDecl;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.tokenizers.KeywordToken;
import org.flasck.flas.tokenizers.Tokenizable;
import org.flasck.flas.tokenizers.TypeNameToken;

public class IntroParser implements TryParsing {

	@Override
	public Object tryParsing(Tokenizable line) {
		if (!line.hasMore())
			return null;
		String kw = KeywordToken.from(line);
		if (kw == null)
			return null; // in the "nothing doing" sense
		
		switch (kw) {
		case "struct": {
			String tn = TypeNameToken.from(line);
			if (tn == null)
				return ErrorResult.oneMessage(line, "invalid type name");
			ErrorResult er = new ErrorResult();
			StructDefn ret = new StructDefn(tn);
			while (line.hasMore()) {
				String ta = TypeNameToken.from(line);
				if (ta == null)
					er.message(line, "invalid type argument");
				else
					ret.add(ta);
			}
			if (er.hasErrors())
				return er;
			return ret;
		}
		case "contract": {
			String tn = TypeNameToken.from(line);
			if (tn == null)
				return ErrorResult.oneMessage(line, "invalid contract name");
			return new ContractDecl(tn);
		}
		case "card": {
			String tn = TypeNameToken.from(line);
			if (tn == null)
				return ErrorResult.oneMessage(line, "invalid contract name");
			return new CardDefiniton(tn);
		}
		case "state":
			return "state";
		case "template":
			return "template";
		}
		
		// we didn't find anything we could handle - "not us"
		return null;
	}

}
