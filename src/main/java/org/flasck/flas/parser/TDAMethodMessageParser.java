package org.flasck.flas.parser;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.AssignMessage;
import org.flasck.flas.parsedForm.SendMessage;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.tokenizers.ExprToken;
import org.flasck.flas.tokenizers.Tokenizable;

public class TDAMethodMessageParser implements TDAParsing {
	private final ErrorReporter errors;
	private final MethodMessagesConsumer builder;

	public TDAMethodMessageParser(ErrorReporter errors, MethodMessagesConsumer builder) {
		this.errors = errors;
		this.builder = builder;
	}

	@Override
	public TDAParsing tryParsing(Tokenizable toks) {
		ExprToken tok = ExprToken.from(toks);
		List<Expr> seen = new ArrayList<>();
		if ("<-".equals(tok.text)) {
			InputPosition pos = tok.location;
			new TDAExpressionParser(errors, t -> {
				seen.add(t);
				SendMessage msg = new SendMessage(pos, t);
				builder.sendMessage(msg);
			}).tryParsing(toks);
			if (seen.isEmpty()) {
				errors.message(toks, "no expression to send");
				return new IgnoreNestedParser();
			}
		} else {
			List<UnresolvedVar> slots = new ArrayList<>();
			boolean haveDot = true;
			while (tok.type == ExprToken.IDENTIFIER) {
				haveDot = false;
				UnresolvedVar v = new UnresolvedVar(tok.location, tok.text);
				slots.add(v);
				tok = ExprToken.from(toks);
				if (tok.type == ExprToken.PUNC && ".".equals(tok.text)) {
					tok = ExprToken.from(toks);
					haveDot = true;
				} else
					break;
			}
			if ("<-".equals(tok.text)) {
				InputPosition pos = tok.location;
				new TDAExpressionParser(errors, t -> {
					seen.add(t);
					AssignMessage msg = new AssignMessage(pos, slots, t);
					builder.assignMessage(msg);
				}).tryParsing(toks);
				if (seen.isEmpty()) {
					errors.message(toks, "no expression to send");
					return new IgnoreNestedParser();
				}
			} else {
				if (haveDot)
					errors.message(toks, "expected identifier");
				else
					errors.message(toks, "expected <-");
				return new IgnoreNestedParser();
			}
		}
		return new NoNestingParser(errors);
	}

	@Override
	public void scopeComplete(InputPosition location) {
		throw new org.zinutils.exceptions.NotImplementedException();
	}

}
