package org.flasck.flas.parser;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.commonBase.Pattern;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.ObjectMethod;
import org.flasck.flas.tokenizers.Tokenizable;
import org.flasck.flas.tokenizers.ValidIdentifierToken;
import org.flasck.flas.tokenizers.VarNameToken;

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
	
	public TDAParsing parseMethod(FunctionNameProvider methodNamer, Tokenizable toks) {
		ValidIdentifierToken var = VarNameToken.from(toks);
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
		FunctionName fnName = methodNamer.functionName(var.location, var.text);
		ObjectMethod meth = new ObjectMethod(var.location, fnName, args);
		builder.addMethod(meth);
		return new TDAMethodMessageParser(errors, meth, new LastActionScopeParser(errors, namer, topLevel, "action"));
	}
}
