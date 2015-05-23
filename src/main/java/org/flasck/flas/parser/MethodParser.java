package org.flasck.flas.parser;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.ErrorResult;
import org.flasck.flas.parsedForm.ContractMethodDecl;
import org.flasck.flas.tokenizers.KeywordToken;
import org.flasck.flas.tokenizers.Tokenizable;
import org.flasck.flas.tokenizers.ValidIdentifierToken;

public class MethodParser implements TryParsing {

	@Override
	public Object tryParsing(Tokenizable line) {
		String ud = KeywordToken.from(line);
		if ("up".equals(ud) || "down".equals(ud))
			;
		else
			return null; // it can't be a method decl
		
		// Read the function name
		String name = ValidIdentifierToken.from(line);
		
		// Collect patterns into an argument
		List<Object> args = new ArrayList<Object>();
		
		// Read all the function's declared arguments
		PatternParser pp = new PatternParser();
		while (line.hasMore()) {
			Object o = pp.tryParsing(line);
			if (o instanceof ErrorResult)
				return o;
			else if (o == null)
				return ErrorResult.oneMessage(line, "could not parse pattern");
			else
				args.add(o);
		}
		
		return new ContractMethodDecl(ud, name, args);
	}

}
