package org.flasck.flas.parser;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.blocker.TDAParsingWithAction;
import org.flasck.flas.commonBase.Pattern;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.errors.ErrorMark;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.ObjectMethod;
import org.flasck.flas.parsedForm.StandaloneMethod;
import org.flasck.flas.parsedForm.StateHolder;
import org.flasck.flas.stories.TDAParserConstructor;
import org.flasck.flas.tokenizers.KeywordToken;
import org.flasck.flas.tokenizers.Tokenizable;
import org.flasck.flas.tokenizers.ValidIdentifierToken;
import org.flasck.flas.tokenizers.VarNameToken;

public class TDAMethodParser extends BlockLocationTracker {
	private final MethodConsumer builder;
	private final FunctionScopeUnitConsumer topLevel;
	private final StateHolder holder;
	private final String reduceAs;
	private final boolean standalone;

	public TDAMethodParser(ErrorReporter errors, FunctionScopeNamer namer, MethodConsumer builder, FunctionScopeUnitConsumer topLevel, StateHolder holder, LocationTracker locTracker, String reduceAs, boolean standalone) {
		super(errors, locTracker);
		this.builder = builder;
		this.topLevel = topLevel;
		this.holder = holder;
		this.reduceAs = reduceAs;
		this.standalone = standalone;
	}
	
	public TDAParsing parseMethod(KeywordToken kw, FunctionNameProvider methodNamer, Tokenizable toks) {
		ErrorMark mark = errors.mark();
		ValidIdentifierToken var = VarNameToken.from(errors, toks);
		if (var == null) {
			errors.message(toks, "no method name provided");
			return new IgnoreNestedParser(errors);
		}
		InputPosition endOf = var.location;
		FunctionName fnName = methodNamer.functionName(var.location, var.text);
		List<Pattern> args = new ArrayList<>();
		TDAPatternParser pp = new TDAPatternParser(errors, new SimpleVarNamer(fnName), p -> {
			args.add(p);
		}, topLevel);
		while (pp.tryParsing(toks) != null)
			;
		if (mark.hasMoreNow()) 
			return new IgnoreNestedParser(errors);
		
		if (toks.hasMoreContent(errors)) {
			errors.message(toks, "extra characters at end of line");
			return new IgnoreNestedParser(errors);
		}
		if (!args.isEmpty()) {
			endOf = args.get(args.size()-1).location();
		}
		if (reduceAs != null) {
			if (kw != null) {
//				errors.logReduction("method-intro", var.location, endOf);
				errors.logReduction(reduceAs, kw.location, endOf);
				tellParent(kw.location);
			} else {
				errors.logReduction(reduceAs, var.location, endOf);
				tellParent(var.location);
			}
		}
		ObjectMethod meth = new ObjectMethod(var.location, fnName, args, null, holder);
		builder.addMethod(meth);
		FunctionScopeNamer nestedNamer = new InnerPackageNamer(fnName);
		TDAMethodGuardParser normal = new TDAMethodGuardParser(errors, meth, new LastActionScopeParser(errors, nestedNamer, topLevel, "action", holder, this), this);
		
		// TODO: this needs more work to handle the "guard" case
		// And the guard case needs more work to handle *its* nested cases
		TDAParsing reducer = new TDAParsingWithAction(normal,
				() -> {
					if (!meth.messages().isEmpty()) {
						reduce(meth.messages().get(0).location(), "method-actions");
					}
				});
		
		if (standalone)
			return new TDAParsingWithAction(
				reducer,
				reduction(kw.location, "standalone-method-definition")
			);
		else
			return reducer;
	}

	public static TDAParserConstructor constructor(FunctionScopeNamer namer, FunctionIntroConsumer sb, FunctionScopeUnitConsumer topLevel, StateHolder holder, LocationTracker locTracker) {
		return new TDAParserConstructor() {
			@Override
			public TDAParsing construct(ErrorReporter errors) {
				return new TDAParsing() {
					@Override
					public TDAParsing tryParsing(Tokenizable toks) {
						KeywordToken kw = KeywordToken.from(errors, toks);
						if (kw == null || !"method".equals(kw.text))
							return null;
						return new TDAMethodParser(errors, namer, m -> topLevel.newStandaloneMethod(errors, new StandaloneMethod(m)), topLevel, holder, locTracker, "method-intro", true).parseMethod(kw, namer, toks);
					}
					
					@Override
					public void scopeComplete(InputPosition location) {
					}
				};
			}
		};
	}
}
