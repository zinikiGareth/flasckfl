package org.flasck.flas.parser;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.FunctionCaseDefn;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.stories.TDAMultiParser;
import org.flasck.flas.tokenizers.ExprToken;
import org.flasck.flas.tokenizers.Tokenizable;

public class TDAFunctionParser implements TDAParsing {
	private final ErrorReporter errors;
	private final ParsedLineConsumer consumer;

	public TDAFunctionParser(ErrorReporter errors, ParsedLineConsumer consumer) {
		this.errors = errors;
		this.consumer = consumer;
	}
	
	@Override
	public TDAParsing tryParsing(Tokenizable line) {
		ExprToken t = ExprToken.from(line);
		if (t == null || t.type != ExprToken.IDENTIFIER)
			return null;
		final FunctionName fname = consumer.functionName(t.location, t.text);
		
		List<Object> args = new ArrayList<>();
		TDAPatternParser pp = new TDAPatternParser(errors, p -> {
			args.add(p);
		});
		// TODO: this should all be a TDAPatternParser, returning to a consumer
		// implemented here that populates args ...
		while (pp.tryParsing(line) != null) {
		}
		
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
		new TDAExpressionParser(errors, e -> {
			consumer.functionCase(new FunctionCaseDefn(fname, args, e));
		}).tryParsing(line);

		// TODO: I don't think this should be quite top - it should allow "as many" intro things (which? not card, but some others such as handler are good to have)
		return TDAMultiParser.top(errors, consumer);
	}

	@Override
	public void scopeComplete(InputPosition location) {
		throw new org.zinutils.exceptions.NotImplementedException();
	}

}
