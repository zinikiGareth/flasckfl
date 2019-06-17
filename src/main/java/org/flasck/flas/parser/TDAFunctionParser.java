package org.flasck.flas.parser;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.VarName;
import org.flasck.flas.errors.ErrorMark;
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
		
		ErrorMark currErr = errors.mark();
		List<Object> args = new ArrayList<>();
		TDAPatternParser pp = new TDAPatternParser(errors, (loc, v) -> new VarName(loc, fname, v), p -> {
			args.add(p);
		}, topLevel);
		while (pp.tryParsing(line) != null)
			;
		if (currErr.hasMoreNow())
			return new IgnoreNestedParser();
		
		// And it resets so that we can pull tok again and see it is an equals sign, or else nothing ...
		InnerPackageNamer innerNamer = new InnerPackageNamer(fname);
		final FunctionIntro intro = new FunctionIntro(fname, args);
		consumer.functionIntro(intro);
		if (!line.hasMore()) {
			return new TDAFunctionGuardedEquationParser(errors, intro, new LastActionScopeParser(errors, innerNamer, topLevel, "case"));
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
			final FunctionCaseDefn fcd = new FunctionCaseDefn(null, e);
			fcds.add(fcd);
			intro.functionCase(fcd);
		}).tryParsing(line);
		
		if (fcds.isEmpty())
			return new IgnoreNestedParser();

		FunctionIntroConsumer assembler = new FunctionAssembler(errors, topLevel);
		return TDAMultiParser.functionScopeUnit(errors, innerNamer, assembler, topLevel);
	}

	@Override
	public void scopeComplete(InputPosition location) {
		consumer.moveOn();
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
