package org.flasck.flas.parser;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Pattern;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.VarName;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.ObjectMethod;
import org.flasck.flas.parsedForm.VarPattern;
import org.flasck.flas.tokenizers.ExprToken;
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
		final FunctionName methName = namer.functionName(name.location, name.text);
		VarPattern handler = null;
		while (toks.hasMore()) {
			ValidIdentifierToken arg = VarNameToken.from(toks);
			if (arg == null) {
				ExprToken tok = ExprToken.from(errors, toks);
				if (tok != null && "->".equals(tok.text)) {
					ValidIdentifierToken h = VarNameToken.from(toks);
					if (h == null) {
						errors.message(toks, "missing or invalid handler name");
						return new IgnoreNestedParser();
					}
					handler = new VarPattern(h.location, new VarName(h.location, methName, h.text));
					topLevel.argument(errors, handler);
					break;
				} else {
					errors.message(toks, "invalid argument name");
					return new IgnoreNestedParser();
				}
			}
			final VarPattern vp = new VarPattern(arg.location, new VarName(arg.location, methName, arg.text));
			args.add(vp);
			topLevel.argument(errors, vp);
		}
		if (toks.hasMore()) {
			errors.message(toks, "syntax error");
			return new IgnoreNestedParser();
		}
		final ObjectMethod meth = new ObjectMethod(name.location, methName, args, handler);
		consumer.addImplementationMethod(meth);
		topLevel.newObjectMethod(errors, meth);
		InnerPackageNamer innerNamer = new InnerPackageNamer(methName);
		LastOneOnlyNestedParser nestedParser = new LastActionScopeParser(errors, innerNamer, topLevel, "action", true);
		return new TDAMethodMessageParser(errors, meth, nestedParser);
	}

	@Override
	public void scopeComplete(InputPosition location) {
	}

}
