package org.flasck.flas.parser;

import org.flasck.flas.errors.ErrorResult;
import org.flasck.flas.parsedForm.StructField;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.tokenizers.Tokenizable;
import org.flasck.flas.tokenizers.VarNameToken;

public class FieldParser implements TryParsing {

	@Override
	public Object tryParsing(Tokenizable line) {
		TypeReference type = (TypeReference) new TypeExprParser().tryParsing(line);
		if (type == null)
			return null; // errors should have been reported already, propagate
		String kw = VarNameToken.from(line);
		if (kw == null)
			return ErrorResult.oneMessage(line, "invalid variable name");
		if (!line.hasMore())
			return new StructField(type, kw);
		line.skipWS();
		String op = line.getTo(2);
		if (!"<-".equals(op))
			return ErrorResult.oneMessage(line, "expected <-");
		Object o = new Expression().tryParsing(line);
		if (o == null)
			return ErrorResult.oneMessage(line, "not a valid expression");
		else if (o instanceof ErrorResult)
			return o;
		else if (line.hasMore())
			return ErrorResult.oneMessage(line, "invalid tokens after expression");
		else
			return new StructField(type, kw, o);
	}

}
