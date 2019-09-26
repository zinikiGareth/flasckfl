package org.flasck.flas.parser;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.FunctionCaseDefn;
import org.flasck.flas.tokenizers.ExprToken;
import org.flasck.flas.tokenizers.Tokenizable;

public class TDAFunctionGuardedEquationParser implements TDAParsing {
	private final ErrorReporter errors;
	private final FunctionGuardedEquationConsumer consumer;
	private final List<FunctionCaseDefn> cases = new ArrayList<>();
	private InputPosition haveDefault;
	private boolean reportedError;
	private LastOneOnlyNestedParser nestedParser;

	public TDAFunctionGuardedEquationParser(ErrorReporter errors, FunctionGuardedEquationConsumer consumer, LastOneOnlyNestedParser nestedParser) {
		this.errors = errors;
		this.consumer = consumer;
		this.nestedParser = nestedParser;
	}

	@Override
	public TDAParsing tryParsing(Tokenizable line) {
		nestedParser.anotherParent();
		InputPosition start = line.realinfo();
		ExprToken tok = ExprToken.from(errors, line);
		if (tok == null || (!tok.text.equals("=") && !tok.text.equals("|"))) {
			errors.message(line, "syntax error");
			return null;
		}
		if (!line.hasMore()) {
			errors.message(line, "function definition requires expression");
			return null;
		}
		
		// Look for and collect a guard, if any
		List<Expr> optionalGuard = new ArrayList<>();
		if (reportedError)
			return null;
		else if (haveDefault != null) {
			errors.message(start, "default case has already been specified");
			reportedError = true;
			return null;
		} else if (tok.text.equals("|")) {
			// it's a guard
			new TDAExpressionParser(errors, e -> {
				optionalGuard.add(e);
			}).tryParsing(line);
			if (errors.hasErrors())
				return null;
	
			tok = ExprToken.from(errors, line);
			if (tok == null || !tok.text.equals("=")) {
				errors.message(line, "syntax error");
				return null;
			}
			if (!line.hasMore()) {
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
		if (cases.isEmpty())
			errors.message(location, "no function cases specified");
	}
}
