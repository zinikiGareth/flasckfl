package org.flasck.flas.parser;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.HandlerName;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.HandlerImplements;
import org.flasck.flas.parsedForm.HandlerLambda;
import org.flasck.flas.parsedForm.StateHolder;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.stories.TDAParserConstructor;
import org.flasck.flas.tokenizers.KeywordToken;
import org.flasck.flas.tokenizers.Tokenizable;
import org.flasck.flas.tokenizers.TypeNameToken;

public class TDAHandlerParser implements TDAParsing {
	private final ErrorReporter errors;
	private final HandlerBuilder builder;
	private final HandlerNameProvider namer;
	private final FunctionScopeUnitConsumer topLevel;
	private final StateHolder holder;

	public TDAHandlerParser(ErrorReporter errors, HandlerBuilder builder, HandlerNameProvider provider, FunctionScopeUnitConsumer topLevel, StateHolder holder) {
		this.errors = errors;
		this.builder = builder;
		this.namer = provider;
		this.topLevel = topLevel;
		this.holder = holder;
	}

	@Override
	public TDAParsing tryParsing(Tokenizable toks) {
		if (!toks.hasMoreContent())
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
		if (!line.hasMoreContent()) {
			errors.message(line, "missing contract reference");
			return new IgnoreNestedParser();
		}
		TypeNameToken tn = TypeNameToken.qualified(line);
		if (tn == null) {
			errors.message(line, "invalid contract reference");
			return new IgnoreNestedParser();
		}
		if (!line.hasMoreContent()) {
			errors.message(line, "missing handler name");
			return new IgnoreNestedParser();
		}
		TypeNameToken named = TypeNameToken.unqualified(line);
		if (named == null) {
			errors.message(line, "invalid handler name");
			return new IgnoreNestedParser();
		}
		List<HandlerLambda> lambdas = new ArrayList<>();
		final HandlerName hn = namer.handlerName(named.text);
		VarNamer vn = new SimpleVarNamer(hn); 
		while (line.hasMoreContent() && !errors.hasErrors()) {
			TDAPatternParser pp = new TDAPatternParser(errors, vn, patt -> lambdas.add(new HandlerLambda(patt, false)), topLevel);
			pp.tryParsing(line);
		}
		for (HandlerLambda hl : lambdas)
			((TopLevelDefinitionConsumer) topLevel).replaceDefinition(hl);
		final HandlerImplements hi = new HandlerImplements(kw, named.location, tn.location, builder, hn, new TypeReference(tn.location, tn.text), inCard, lambdas);
		if (builder != null)
			builder.newHandler(errors, hi);
		topLevel.newHandler(errors, hi);
		return new TDAImplementationMethodsParser(errors, (loc, text) -> FunctionName.handlerMethod(loc, hn, text), hi, topLevel, hi);
	}

	public static TDAParserConstructor constructor(HandlerBuilder builder, HandlerNameProvider namer, FunctionScopeUnitConsumer topLevel, StateHolder holder) {
		return new TDAParserConstructor() {
			@Override
			public TDAParsing construct(ErrorReporter errors) {
				return new TDAHandlerParser(errors, builder, namer, topLevel, holder);
			}
		};
	}

}
