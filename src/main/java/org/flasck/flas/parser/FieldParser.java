package org.flasck.flas.parser;

import org.flasck.flas.errors.ErrorResult;
import org.flasck.flas.parsedForm.StructField;
import org.flasck.flas.tokenizers.Tokenizable;
import org.flasck.flas.tokenizers.ValidIdentifierToken;
import org.flasck.flas.tokenizers.VarNameToken;
import org.flasck.flas.typechecker.Type;

public class FieldParser implements TryParsing {

	@Override
	public Object tryParsing(Tokenizable line) {
		Type type = (Type) new TypeExprParser().tryParsing(line);
		if (type == null)
			return null; // errors should have been reported already, propagate
		ValidIdentifierToken kw = VarNameToken.from(line);
		if (kw == null)
			return ErrorResult.oneMessage(line, "invalid variable name");
		if (!line.hasMore())
			return new StructField(type, kw.text);
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
			return new StructField(type, kw.text, o);
	}

}
