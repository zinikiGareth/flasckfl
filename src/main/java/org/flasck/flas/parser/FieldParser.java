package org.flasck.flas.parser;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.errors.ErrorResult;
import org.flasck.flas.parsedForm.StructField;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.tokenizers.KeywordToken;
import org.flasck.flas.tokenizers.Tokenizable;
import org.flasck.flas.tokenizers.ValidIdentifierToken;
import org.flasck.flas.tokenizers.VarNameToken;

public class FieldParser implements TryParsing {

	public static final int CARD = 1;
	public static final int OBJECT = 2;
	private final int mode;

	public FieldParser(int mode) {
		this.mode = mode;
	}

	@Override
	public Object tryParsing(Tokenizable line) {
		boolean accessor = false;
		if (mode == OBJECT) {
			int mark = line.at();
			KeywordToken tok = KeywordToken.from(line);
			if (tok != null && tok.text.equals("acor"))
				accessor = true;
			else
				line.reset(mark);
		}
		TypeReference type = (TypeReference) new TypeExprParser().tryParsing(line);
		if (type == null)
			return null; // errors should have been reported already, propagate
		ValidIdentifierToken kw = VarNameToken.from(line);
		if (kw == null)
			return ErrorResult.oneMessage(line, "invalid variable name");
		if (!line.hasMore())
			return new StructField(kw.location, accessor, type, kw.text);
		line.skipWS();
		InputPosition assOp = line.realinfo();
		String op = line.getTo(2);
		if (!"<-".equals(op))
			return ErrorResult.oneMessage(line, "expected <-");
		assOp.endAt(line.at());
		Object o = new Expression().tryParsing(line);
		if (o == null)
			return ErrorResult.oneMessage(line, "not a valid expression");
		else if (o instanceof ErrorResult)
			return o;
		else if (line.hasMore())
			return ErrorResult.oneMessage(line, "invalid tokens after expression");
		else
			return new StructField(kw.location, assOp, accessor, type, kw.text, o);
	}

}
