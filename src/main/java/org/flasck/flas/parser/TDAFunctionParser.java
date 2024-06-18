package org.flasck.flas.parser;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.blocker.TDAParsingWithAction;
import org.flasck.flas.commonBase.Pattern;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.compiler.ParsingPhase;
import org.flasck.flas.errors.ErrorMark;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.FunctionCaseDefn;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.parsedForm.StateHolder;
import org.flasck.flas.stories.TDAParserConstructor;
import org.flasck.flas.tokenizers.ExprToken;
import org.flasck.flas.tokenizers.Tokenizable;

public class TDAFunctionParser extends BlockLocationTracker implements TDAParsing {
	private final FunctionNameProvider functionNamer;
	private final FunctionCaseNameProvider functionCaseNamer;
	private final FunctionIntroConsumer consumer;
	private final FunctionScopeUnitConsumer topLevel;
	private final StateHolder holder;

	public TDAFunctionParser(ErrorReporter errors, FunctionNameProvider functionNamer, FunctionCaseNameProvider functionCaseNamer, FunctionIntroConsumer consumer, FunctionScopeUnitConsumer topLevel, StateHolder holder, LocationTracker locTracker) {
		super(errors, locTracker);
		this.functionNamer = functionNamer;
		this.functionCaseNamer = functionCaseNamer;
		this.consumer = consumer;
		this.topLevel = topLevel;
		this.holder = holder;
	}
	
	@Override
	public TDAParsing tryParsing(Tokenizable line) {
		ExprToken t = ExprToken.from(errors, line);
		if (t == null || t.type != ExprToken.IDENTIFIER) {
			consumer.moveOn();
			return null;
		}
		final FunctionName fname = functionNamer.functionName(t.location, t.text);
		final FunctionName fcase = functionCaseNamer.functionCaseName(t.location, t.text, consumer.nextCaseNumber(fname));
		
		ErrorMark currErr = errors.mark();
		List<Pattern> args = new ArrayList<>();
		TDAPatternParser pp = new TDAPatternParser(errors, new SimpleVarNamer(fcase), p -> {
			args.add(p);
		}, topLevel);
		while (pp.tryParsing(line, currErr) != null)
			;
		if (currErr.hasMoreNow())
			return new IgnoreNestedParser(errors);
		
		// And it resets so that we can pull tok again and see it is an equals sign, or else nothing ...
		InnerPackageNamer innerNamer = new InnerPackageNamer(fcase);
		final FunctionIntro intro = new FunctionIntro(fcase, args);
		consumer.functionIntro(intro);
		if (!line.hasMoreContent(errors)) {
			consumer.hasGuards(true);
//			errors.logReduction("function-intro-no-expr", intro.location, line.realinfo());
			TDAFunctionGuardedEquationParser inner = new TDAFunctionGuardedEquationParser(errors, intro, line.realinfo(), intro, new LastActionScopeParser(errors, innerNamer, topLevel, "case", holder, this), this);
			return new TDAParsingWithAction(
				inner,
				() -> { 
					consumer.done();
					if (intro.cases().isEmpty()) {
						// this is an error which I think will be reported elsewhere
					} else if (intro.cases().size() > 1) {
						if (intro.cases().get(intro.cases().size()-1).guard == null) {
							reduce(intro.cases().get(0).location, "guarded-equations-with-default");
						} else
							reduce(intro.cases().get(0).location, "guarded-equations");
						reduce(t.location, "guarded-function-case-definition");
					} else {
						if (intro.cases().get(0).guard == null) {
							reduce(t.location, "degenerate-guarded-function-case-definition");
						} else {
							reduce(intro.cases().get(0).location, "guarded-equations");
							reduce(t.location, "guarded-function-case-definition");
						}
					}
				}
			);
		}
		ExprToken tok = ExprToken.from(errors, line);
		if (tok == null) {
			return null;
		} else if (!tok.text.equals("=")) {
			return null;
		}
		if (!line.hasMoreContent(errors)) {
			return null;
		}
		List<FunctionCaseDefn> fcds = new ArrayList<>();
		new TDAExpressionParser(errors, e -> {
//			errors.logReduction("expression", e, e);
			final FunctionCaseDefn fcd = new FunctionCaseDefn(e.location(), intro, null, e);
			fcds.add(fcd);
			intro.functionCase(fcd);
		}).tryParsing(line);
		
		if (fcds.isEmpty())
			return new IgnoreNestedParser(errors);

		if (line.hasMoreContent(errors)) {
			errors.message(line, "syntax error");
			return new IgnoreNestedParser(errors);
		}

		consumer.hasGuards(false);
		super.updateLoc(t.location);
		FunctionAssembler assembler = new FunctionAssembler(errors, topLevel, holder, this);
		return new TDAParsingWithAction(
			ParsingPhase.functionScopeUnit(errors, innerNamer, assembler, topLevel, holder, this),
			() -> { 
				consumer.done(); 
				reduce(t.location, "simple-function-case-definition");
			}
		);
	}

	@Override
	public void choseOther() {
		consumer.moveOn();
	}
	
	@Override
	public void scopeComplete(InputPosition location) {
		consumer.moveOn();
	}

	public static TDAParserConstructor constructor(FunctionNameProvider namer, FunctionCaseNameProvider caseNamer, FunctionIntroConsumer consumer, FunctionScopeUnitConsumer topLevel, StateHolder holder, LocationTracker locTracker) {
		return new TDAParserConstructor() {
			@Override
			public TDAParsing construct(ErrorReporter errors) {
				return new TDAFunctionParser(errors, namer, caseNamer, consumer, topLevel, holder, locTracker);
			}
		};
	}
}
