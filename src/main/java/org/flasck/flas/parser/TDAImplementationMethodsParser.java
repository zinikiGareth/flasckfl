package org.flasck.flas.parser;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Pattern;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.HandlerName;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.ObjectMethod;
import org.flasck.flas.parsedForm.VarPattern;
import org.flasck.flas.tokenizers.Tokenizable;
import org.flasck.flas.tokenizers.ValidIdentifierToken;
import org.flasck.flas.tokenizers.VarNameToken;

public class TDAImplementationMethodsParser implements TDAParsing {
	private final ErrorReporter errors;
	private final ImplementationMethodConsumer consumer;
	private final FunctionNameProvider namer;
	private final FunctionScopeUnitConsumer topLevel;

	public TDAImplementationMethodsParser(ErrorReporter errors, FunctionNameProvider namer, ImplementationMethodConsumer consumer, FunctionScopeUnitConsumer topLevel) {
		this.errors = errors;
		this.namer = namer;
		this.consumer = consumer;
		this.topLevel = topLevel;
	}

	@Override
	public TDAParsing tryParsing(Tokenizable toks) {
		if (!toks.hasMore())
			return null;
		ValidIdentifierToken name = VarNameToken.from(toks);
		if (name == null) {
			errors.message(toks, "invalid method name");
			return new IgnoreNestedParser();
		}
		List<Pattern> args = new ArrayList<>();
		while (toks.hasMore()) {
			ValidIdentifierToken arg = VarNameToken.from(toks);
			if (arg == null) {
				errors.message(toks, "invalid argument name");
				return new IgnoreNestedParser();
			}
			args.add(new VarPattern(arg.location, arg.text));
		}
		final ObjectMethod meth = new ObjectMethod(name.location, namer.functionName(name.location, name.text), args);
		consumer.addImplementationMethod(meth);
		FunctionNameProvider scopeNamer = (loc, text) -> FunctionName.function(loc, meth.name(), text);
		final HandlerNameProvider handlerNamer = text -> new HandlerName(meth.name(), text);
		LastOneOnlyNestedParser nestedParser = new LastActionScopeParser(errors, scopeNamer, handlerNamer, topLevel, "action");
		return new TDAMethodMessageParser(errors, meth, nestedParser);
	}

	@Override
	public void scopeComplete(InputPosition location) {
	}

}
