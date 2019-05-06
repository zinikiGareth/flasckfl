package org.flasck.flas.parser;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.tokenizers.Tokenizable;

public class TDAObjectElementsParser implements TDAParsing {
	private final ErrorReporter errors;
	private final ObjectElementsConsumer builder;

	public TDAObjectElementsParser(ErrorReporter errors, ObjectElementsConsumer od) {
		this.errors = errors;
		this.builder = od;
	}

	@Override
	public TDAParsing tryParsing(Tokenizable toks) {
		/*
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
		ReturnParser ret = new ReturnParser();
		if (!toks.hasMore()) {
			builder.addField(new StructField(kw.location, accessor, type, kw.text));
			ret.noNest(errors);
		} else {
			toks.skipWS();
			InputPosition assOp = toks.realinfo();
			String op = toks.getTo(2);
			if (!"<-".equals(op)) {
				errors.message(toks, "expected <- or end of line");
				return null;
			}
			assOp.endAt(toks.at());
			new TDAExpressionParser(errors, expr -> { if (!errors.hasErrors()) {ret.noNest(errors); builder.addField(new StructField(kw.location, assOp, accessor, type, kw.text, expr));}}).tryParsing(toks);
		}
		return ret.get();
		*/
		return null;
	}

	@Override
	public void scopeComplete(InputPosition location) {
	}
}
