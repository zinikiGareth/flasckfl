package org.flasck.flas.parser;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.SendMessage;
import org.flasck.flas.tokenizers.ExprToken;
import org.flasck.flas.tokenizers.Tokenizable;
import org.zinutils.exceptions.NotImplementedException;

public class TDAMethodMessageParser implements TDAParsing {
	private final ErrorReporter errors;
	private final MethodMessagesConsumer builder;

	public TDAMethodMessageParser(ErrorReporter errors, MethodMessagesConsumer builder) {
		this.errors = errors;
		this.builder = builder;
	}

	@Override
	public TDAParsing tryParsing(Tokenizable toks) {
		InputPosition start = toks.realinfo();
		ExprToken tok = ExprToken.from(toks);
		if ("<-".equals(tok.text)) {
			InputPosition pos = start.copySetEnd(toks.at());
			new TDAExpressionParser(errors, t -> {
				SendMessage msg = new SendMessage(pos, t);
				builder.sendMessage(msg);
			}).tryParsing(toks);
			return new NoNestingParser(errors);
		} else
			throw new NotImplementedException();
	}

	@Override
	public void scopeComplete(InputPosition location) {
		throw new org.zinutils.exceptions.NotImplementedException();
	}

}
