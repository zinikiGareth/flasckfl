package org.flasck.flas.parser;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Pattern;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.VarName;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.ObjectMethod;
import org.flasck.flas.parsedForm.StandaloneMethod;
import org.flasck.flas.stories.TDAParserConstructor;
import org.flasck.flas.tokenizers.KeywordToken;
import org.flasck.flas.tokenizers.Tokenizable;
import org.flasck.flas.tokenizers.ValidIdentifierToken;
import org.flasck.flas.tokenizers.VarNameToken;

public class TDAMethodParser {
	private final ErrorReporter errors;
	private final FunctionScopeNamer namer;
	private final MethodConsumer builder;
	private final FunctionScopeUnitConsumer topLevel;

	public TDAMethodParser(ErrorReporter errors, FunctionScopeNamer namer, MethodConsumer builder, FunctionScopeUnitConsumer topLevel) {
		this.errors = errors;
		this.namer = namer;
		this.builder = builder;
		this.topLevel = topLevel;
	}
	
	public TDAParsing parseMethod(FunctionNameProvider methodNamer, Tokenizable toks) {
		ValidIdentifierToken var = VarNameToken.from(toks);
		if (var == null) {
			errors.message(toks, "no method name provided");
			return new IgnoreNestedParser();
		}
		FunctionName fnName = methodNamer.functionName(var.location, var.text);
		List<Pattern> args = new ArrayList<>();
		TDAPatternParser pp = new TDAPatternParser(errors, (loc, v) -> new VarName(loc, fnName, v), p -> {
			args.add(p);
		}, topLevel);
		while (pp.tryParsing(toks) != null)
			;
		if (errors.hasErrors()) 
			return new IgnoreNestedParser();
		
		if (toks.hasMore()) {
			errors.message(toks, "extra characters at end of line");
			return new IgnoreNestedParser();
		}
		ObjectMethod meth = new ObjectMethod(var.location, fnName, args, null);
		builder.addMethod(meth);
		return new TDAMethodMessageParser(errors, meth, new LastActionScopeParser(errors, namer, topLevel, "action"));
	}

	public static TDAParserConstructor constructor(FunctionScopeNamer namer, FunctionIntroConsumer sb, FunctionScopeUnitConsumer topLevel) {
		return new TDAParserConstructor() {
			@Override
			public TDAParsing construct(ErrorReporter errors) {
				return new TDAParsing() {
					@Override
					public TDAParsing tryParsing(Tokenizable toks) {
						KeywordToken kw = KeywordToken.from(toks);
						if (kw == null || !"method".equals(kw.text))
							return null;
						return new TDAMethodParser(errors, namer, m -> topLevel.newStandaloneMethod(errors, new StandaloneMethod(m)), topLevel).parseMethod(namer, toks);
					}
					
					@Override
					public void scopeComplete(InputPosition location) {
					}
				};
			}
		};
	}
}
