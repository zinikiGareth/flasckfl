package org.flasck.flas.parser;

import java.util.ArrayList;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.HandlerImplements;
import org.flasck.flas.tokenizers.Tokenizable;
import org.flasck.flas.tokenizers.TypeNameToken;

public class TDAHandlerParser {
	private final ErrorReporter errors;
	private final FunctionScopeUnitConsumer builder;
	private final HandlerNameProvider namer;

	public TDAHandlerParser(ErrorReporter errors, FunctionScopeUnitConsumer builder, HandlerNameProvider provider) {
		this.errors = errors;
		this.builder = builder;
		this.namer = provider;
	}

	public TDAParsing parseHandler(InputPosition kw, boolean inCard, Tokenizable line) {
		if (!line.hasMore()) {
			errors.message(line, "missing contract reference");
			return new IgnoreNestedParser();
		}
		TypeNameToken tn = TypeNameToken.qualified(line);
		if (tn == null) {
			errors.message(line, "invalid contract reference");
			return new IgnoreNestedParser();
		}
		if (!line.hasMore()) {
			errors.message(line, "missing handler name");
			return new IgnoreNestedParser();
		}
		TypeNameToken named = TypeNameToken.unqualified(line);
		if (named == null) {
			errors.message(line, "invalid handler name");
			return new IgnoreNestedParser();
		}
		ArrayList<Object> lambdas = new ArrayList<Object>();
		while (line.hasMore()) {
			PatternParser pp = new PatternParser();
			Object patt = pp.tryParsing(line);
			if (patt == null) {
				errors.message(line, "invalid contract argument pattern");
				return new IgnoreNestedParser();
			}
			lambdas.add(patt);
		}
		builder.newHandler(new HandlerImplements(kw, named.location, tn.location, namer.provide(named.text), tn.text, inCard, lambdas));
		return new TDAManyMethodsParser();
	}

}