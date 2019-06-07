package org.flasck.flas.parser;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.commonBase.Pattern;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.ObjectMethod;
import org.flasck.flas.tokenizers.Tokenizable;

public class TDAMethodParser {
	private final ErrorReporter errors;
	private final FunctionNameProvider namer;
	private final MethodConsumer builder;
	private final FunctionScopeUnitConsumer topLevel;

	public TDAMethodParser(ErrorReporter errors, FunctionNameProvider namer, MethodConsumer builder, FunctionScopeUnitConsumer topLevel) {
		this.errors = errors;
		this.namer = namer;
		this.builder = builder;
		this.topLevel = topLevel;
	}
	
	public TDAParsing parseMethod(FunctionName fnName, Tokenizable toks) {
		List<Pattern> args = new ArrayList<>();
		TDAPatternParser pp = new TDAPatternParser(errors, p -> {
			args.add(p);
		});
		while (pp.tryParsing(toks) != null)
			;
		if (toks.hasMore()) {
			errors.message(toks, "extra characters at end of line");
			return new IgnoreNestedParser();
		}
		ObjectMethod meth = new ObjectMethod(fnName.location, fnName, args);
		builder.addMethod(meth);
		return new TDAMethodMessageParser(errors, meth, new LastActionScopeParser(errors, namer, topLevel, "action"));
	}
}
