package org.flasck.flas.parser;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.errors.ErrorResult;
import org.flasck.flas.errors.FLASError;
import org.flasck.flas.parsedForm.StructField;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.tokenizers.Tokenizable;
import org.flasck.flas.tokenizers.ValidIdentifierToken;
import org.flasck.flas.tokenizers.VarNameToken;

public class TDAStructFieldParser implements TDAParsing {
	private final ErrorReporter errors;
	private final StructFieldConsumer builder;

	public TDAStructFieldParser(ErrorReporter errors, StructFieldConsumer builder) {
		this.errors = errors;
		this.builder = builder;
	}

	@Override
	public TDAParsing tryParsing(Tokenizable toks) {
		// TODO: figure accessor for object case
		final boolean accessor = false;
		TypeReference type = (TypeReference) new TypeExprParser().tryParsing(toks);
		if (type == null) {
			errors.message(toks, "field must have a valid type definition");
			return null;
		}
		ValidIdentifierToken kw = VarNameToken.from(toks);
		if (kw == null) {
			errors.message(toks, "field must have a valid field name");
			return null;
		}
		if (kw.text.equals("id")) {
			errors.message(toks, "'id' is a reserved field name");
			return null;
		}
		if (!toks.hasMore()) {
			builder.addField(new StructField(kw.location, accessor, type, kw.text));
			return null;
		} else {
			toks.skipWS();
			InputPosition assOp = toks.realinfo();
			String op = toks.getTo(2);
			if (!"<-".equals(op)) {
				errors.message(toks, "expected <- or end of line");
				return null;
			}
			assOp.endAt(toks.at());
			// TDA
			Object o = new Expression().tryParsing(toks);
			if (o == null)
				errors.message(toks, "not a valid expression");
			else if (o instanceof ErrorReporter) {
				errors.merge((ErrorReporter)o);
				ErrorResult er = (ErrorResult)o;
				for (FLASError e : er)
					errors.message(e);
			} else if (toks.hasMore())
				errors.message(toks, "invalid tokens after expression");
			else
				builder.addField(new StructField(kw.location, assOp, accessor, type, kw.text, o));
			return null;
		}
	}
}
