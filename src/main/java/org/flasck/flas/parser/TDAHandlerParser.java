package org.flasck.flas.parser;

import java.util.ArrayList;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.HandlerName;
import org.flasck.flas.commonBase.names.VarName;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.HandlerImplements;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.stories.TDAParserConstructor;
import org.flasck.flas.tokenizers.KeywordToken;
import org.flasck.flas.tokenizers.Tokenizable;
import org.flasck.flas.tokenizers.TypeNameToken;

public class TDAHandlerParser implements TDAParsing {
	private final ErrorReporter errors;
	private final FunctionScopeUnitConsumer builder;
	private final HandlerNameProvider namer;
	private final FunctionScopeUnitConsumer topLevel;

	public TDAHandlerParser(ErrorReporter errors, FunctionScopeUnitConsumer builder, HandlerNameProvider provider, FunctionScopeUnitConsumer topLevel) {
		this.errors = errors;
		this.builder = builder;
		this.namer = provider;
		this.topLevel = topLevel;
	}

	@Override
	public TDAParsing tryParsing(Tokenizable toks) {
		if (!toks.hasMore())
			return null;
		KeywordToken kw = KeywordToken.from(toks);
		if (kw == null || !kw.text.equals("handler"))
			return null; // in the "nothing doing" sense

		return parseHandler(kw.location, false, toks);
	}

	@Override
	public void scopeComplete(InputPosition location) {
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
		final HandlerName hn = namer.handlerName(named.text);
		VarNamer vn = (loc, t) -> new VarName(loc, hn, t); 
		while (line.hasMore() && !errors.hasErrors()) {
			TDAPatternParser pp = new TDAPatternParser(errors, vn, patt -> lambdas.add(patt), topLevel);
			pp.tryParsing(line);
		}
		final HandlerImplements hi = new HandlerImplements(kw, named.location, tn.location, hn, new TypeReference(tn.location, tn.text), inCard, lambdas);
		builder.newHandler(hi);
		return new TDAImplementationMethodsParser(errors, (loc, text) -> FunctionName.handlerMethod(loc, hn, text), vn, hi, topLevel);
	}

	public static TDAParserConstructor constructor(HandlerNameProvider namer, FunctionScopeUnitConsumer topLevel) {
		return new TDAParserConstructor() {
			@Override
			public TDAParsing construct(ErrorReporter errors) {
				return new TDAHandlerParser(errors, topLevel, namer, topLevel);
			}
		};
	}

}
