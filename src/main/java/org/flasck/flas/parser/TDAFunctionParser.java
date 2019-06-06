package org.flasck.flas.parser;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.FunctionCaseDefn;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.stories.TDAMultiParser;
import org.flasck.flas.stories.TDAParserConstructor;
import org.flasck.flas.tokenizers.ExprToken;
import org.flasck.flas.tokenizers.Tokenizable;

public class TDAFunctionParser implements TDAParsing {
	private final ErrorReporter errors;
	private final FunctionNameProvider functionNamer;
	private final FunctionIntroConsumer consumer;
	private final FunctionScopeUnitConsumer topLevel;

	public TDAFunctionParser(ErrorReporter errors, FunctionNameProvider functionNamer, FunctionIntroConsumer consumer, FunctionScopeUnitConsumer topLevel) {
		this.errors = errors;
		this.functionNamer = functionNamer;
		this.consumer = consumer;
		this.topLevel = topLevel;
	}
	
	@Override
	public TDAParsing tryParsing(Tokenizable line) {
		ExprToken t = ExprToken.from(line);
		if (t == null || t.type != ExprToken.IDENTIFIER)
			return null;
		final FunctionName fname = functionNamer.functionName(t.location, t.text);
		
		List<Object> args = new ArrayList<>();
		TDAPatternParser pp = new TDAPatternParser(errors, p -> {
			args.add(p);
		});
		while (pp.tryParsing(line) != null)
			;
		
		// And it resets so that we can pull tok again and see it is an equals sign, or else nothing ...
		if (!line.hasMore()) {
			consumer.functionIntro(new FunctionIntro(fname, args));
			return new TDAFunctionCaseParser(errors, consumer);
		}
		ExprToken tok = ExprToken.from(line);
		if (tok == null || !tok.text.equals("=")) {
			errors.message(line, "syntax error");
			return null;
		}
		if (!line.hasMore()) {
			errors.message(line, "function definition requires expression");
			return null;
		}
		List<FunctionCaseDefn> fcds = new ArrayList<>();
		new TDAExpressionParser(errors, e -> {
			final FunctionCaseDefn fcd = new FunctionCaseDefn(fname, args, e);
			fcds.add(fcd);
			consumer.functionCase(fcd);
		}).tryParsing(line);
		
		if (fcds.isEmpty())
			return new IgnoreNestedParser();

		return TDAMultiParser.functionScopeUnit(errors, functionNamer, topLevel, topLevel);
	}

	@Override
	public void scopeComplete(InputPosition location) {
		throw new org.zinutils.exceptions.NotImplementedException();
	}

	public static TDAParserConstructor constructor(FunctionNameProvider namer, FunctionIntroConsumer consumer, FunctionScopeUnitConsumer topLevel) {
		return new TDAParserConstructor() {
			@Override
			public TDAParsing construct(ErrorReporter errors) {
				return new TDAFunctionParser(errors, namer, consumer, topLevel);
			}
		};
	}

}
