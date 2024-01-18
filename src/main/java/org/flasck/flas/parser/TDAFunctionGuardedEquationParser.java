package org.flasck.flas.parser;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.errors.ErrorMark;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.FunctionCaseDefn;
import org.flasck.flas.tokenizers.ExprToken;
import org.flasck.flas.tokenizers.Tokenizable;

public class TDAFunctionGuardedEquationParser implements TDAParsing {
	private final ErrorReporter errors;
	private final InputPosition introStart;
	private final FunctionGuardedEquationConsumer consumer;
	private final List<FunctionCaseDefn> cases = new ArrayList<>();
	private InputPosition haveDefault;
	private LastOneOnlyNestedParser nestedParser;
	private boolean reportedDefault;

	public TDAFunctionGuardedEquationParser(ErrorReporter errors, InputPosition introStart, FunctionGuardedEquationConsumer consumer, LastOneOnlyNestedParser nestedParser) {
		this.errors = errors;
		this.introStart = introStart;
		this.consumer = consumer;
		this.nestedParser = nestedParser;
	}

	@Override
	public TDAParsing tryParsing(Tokenizable line) {
		ErrorMark mark = errors.mark();
		nestedParser.anotherParent();
		InputPosition start = line.realinfo();
		ExprToken tok = ExprToken.from(errors, line);
		if (tok == null) {
			errors.message(line, "syntax error in function case definition");
			return null;
		}
		if (!tok.text.equals("=") && !tok.text.equals("|")) {
			errors.message(tok.location, "syntax error in function case definition");
			return null;
		}
		if (!line.hasMoreContent(errors)) {
			errors.message(line, "function definition requires expression");
			return null;
		}
		
		// Look for and collect a guard, if any
		List<Expr> optionalGuard = new ArrayList<>();
		if (mark.hasMoreNow())
			return null;
		else if (haveDefault != null) {
			if (!reportedDefault) {
				errors.message(start, "default case has already been specified");
				reportedDefault = true;
			}
			return null;
		} else if (tok.text.equals("|")) {
			// it's a guard
			new TDAExpressionParser(errors, e -> {
				optionalGuard.add(e);
			}).tryParsing(line);
			if (mark.hasMoreNow())
				return null;
	
			tok = ExprToken.from(errors, line);
			if (tok == null || !tok.text.equals("=")) {
				errors.message(line, "syntax error");
				return null;
			}
			if (!line.hasMoreContent(errors)) {
				errors.message(line, "function definition requires expression");
				return null;
			}
		} else {
			haveDefault = start;
		}

		// Collect the expression
		List<FunctionCaseDefn> fcds = new ArrayList<>();
		new TDAExpressionParser(errors, e -> {
			Expr guard = optionalGuard.isEmpty() ? null : optionalGuard.get(0);
			final FunctionCaseDefn fcd = new FunctionCaseDefn(guard, e);
			fcds.add(fcd);
			consumer.functionCase(fcd);
			cases.add(fcd);
		}).tryParsing(line);
		
		return nestedParser;
	}

	@Override
	public void scopeComplete(InputPosition location) {
		if (cases.isEmpty() && !errors.hasErrors()) {
			errors.message(introStart, "no function cases specified");
			consumer.breakIt();
		}
	}
}
