package org.flasck.flas.parser;

import org.flasck.flas.parsedForm.StructField;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.tokenizers.Tokenizable;
import org.flasck.flas.tokenizers.VarNameToken;

public class StructFieldParser implements TryParsing {

	@Override
	public Object tryParsing(Tokenizable line) {
		Object type = new TypeExprParser().tryParsing(line);
		if (type == null)
			return null; // errors should have been reported already, propagate
		String kw = VarNameToken.from(line);
		if (kw == null || line.hasMore())
			return null; // two (separate?) error conditions
		return new StructField(type, kw);
	}

}
