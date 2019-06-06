package org.flasck.flas.parser;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.FunctionCaseDefn;
import org.flasck.flas.tokenizers.ExprToken;
import org.flasck.flas.tokenizers.Tokenizable;

public class TDAFunctionCaseParser implements TDAParsing {
	private final ErrorReporter errors;
	private final FunctionIntroConsumer consumer;
	private final FunctionName fname;
	private final List<Object> args;

	public TDAFunctionCaseParser(ErrorReporter errors, FunctionIntroConsumer consumer, FunctionName fname, List<Object> args) {
		this.errors = errors;
		this.consumer = consumer;
		this.fname = fname;
		this.args = args;
	}

	@Override
	public TDAParsing tryParsing(Tokenizable line) {
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
		
		// TODO: should be a LOONP
		return new IgnoreNestedParser();
	}

	@Override
	public void scopeComplete(InputPosition location) {
		errors.message(location, "no function cases specified");
	}
}
