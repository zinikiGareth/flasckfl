package org.flasck.flas.parser;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Locatable;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.HandlerName;
import org.flasck.flas.errors.ErrorMark;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.HandlerImplements;
import org.flasck.flas.parsedForm.HandlerLambda;
import org.flasck.flas.parsedForm.StateHolder;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.stories.TDAParserConstructor;
import org.flasck.flas.tc3.NamedType;
import org.flasck.flas.tokenizers.KeywordToken;
import org.flasck.flas.tokenizers.Tokenizable;
import org.flasck.flas.tokenizers.TypeNameToken;

public class TDAHandlerParser implements TDAParsing, LocationTracker {
	private final ErrorReporter errors;
	private final HandlerBuilder builder;
	private final HandlerNameProvider namer;
	private final FunctionScopeUnitConsumer topLevel;
	private final StateHolder holder;
	private KeywordToken kw;
	private InputPosition lastInner;
	private LocationTracker locTracker;

	public TDAHandlerParser(ErrorReporter errors, HandlerBuilder builder, HandlerNameProvider provider, FunctionScopeUnitConsumer topLevel, StateHolder holder, LocationTracker tracker) {
		this.errors = errors;
		this.builder = builder;
		this.namer = provider;
		this.topLevel = topLevel;
		this.holder = holder;
		this.locTracker = tracker;
	}

	@Override
	public TDAParsing tryParsing(Tokenizable toks) {
		if (!toks.hasMoreContent(errors))
			return null;
		kw = KeywordToken.from(errors, toks);
		if (kw == null || !kw.text.equals("handler")) {
			kw = null;
			return null; // in the "nothing doing" sense
		}

		lastInner = kw.location;
		return parseHandler(kw.location, false, toks);
	}


	@Override
	public void updateLoc(InputPosition location) {
		if (location != null && (lastInner == null || location.compareTo(lastInner) > 0))
			lastInner = location;
	}

	@Override
	public void choseOther() {
		logreduction();
	}
	
	@Override
	public void scopeComplete(InputPosition location) {
		logreduction();
	}
	
	private void logreduction() {
		if (kw == null)
			return;
		
		errors.logReduction("handler-with-inner-block", kw.location, lastInner);
		if (locTracker != null) {
			locTracker.updateLoc(kw.location);
		}
		kw = null;
	}

	public TDAParsing parseHandler(InputPosition kw, boolean inCard, Tokenizable line) {
		ErrorMark mark = errors.mark();
		if (!line.hasMoreContent(errors)) {
			errors.message(line, "missing contract reference");
			return new IgnoreNestedParser(errors);
		}
		TypeNameToken tn = TypeNameToken.qualified(errors, line);
		if (tn == null) {
			errors.message(line, "invalid contract reference");
			return new IgnoreNestedParser(errors);
		}
		if (!line.hasMoreContent(errors)) {
			errors.message(line, "missing handler name");
			return new IgnoreNestedParser(errors);
		}
		TypeNameToken named = TypeNameToken.unqualified(errors, line);
		if (named == null) {
			errors.message(line, "invalid handler name");
			return new IgnoreNestedParser(errors);
		}
		Locatable upto = named;
		List<HandlerLambda> lambdas = new ArrayList<>();
		final HandlerName hn = namer.handlerName(named.text);
		VarNamer vn = new SimpleVarNamer(hn); 
		while (line.hasMoreContent(errors) && !mark.hasMoreNow()) {
			TDAPatternParser pp = new TDAPatternParser(errors, vn, patt -> lambdas.add(new HandlerLambda(patt, false)), topLevel);
			pp.tryParsing(line);
		}
		for (HandlerLambda hl : lambdas) {
			((TopLevelDefinitionConsumer) topLevel).replaceDefinition(hl);
			upto = hl;
		}
		errors.logReduction("handler-intro", kw, upto.location());
		final HandlerImplements hi = new HandlerImplements(kw, named.location, tn.location, (NamedType) holder, hn, new TypeReference(tn.location, tn.text), inCard, lambdas);
		if (builder != null)
			builder.newHandler(errors, hi);
		topLevel.newHandler(errors, hi);
		return new TDAImplementationMethodsParser(errors, (loc, text) -> FunctionName.handlerMethod(loc, hn, text), hi, topLevel, hi, this);
	}

	public static TDAParserConstructor constructor(HandlerBuilder builder, HandlerNameProvider namer, FunctionScopeUnitConsumer topLevel, StateHolder holder, LocationTracker locTracker) {
		return new TDAParserConstructor() {
			@Override
			public TDAParsing construct(ErrorReporter errors) {
				return new TDAHandlerParser(errors, builder, namer, topLevel, holder, locTracker);
			}
		};
	}
}
