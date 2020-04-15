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
	private final LastOneOnlyNestedParser nestedParser;

	public TDAMethodMessageParser(ErrorReporter errors, MethodMessagesConsumer builder, LastOneOnlyNestedParser nestedParser) {
		this.errors = errors;
		this.builder = builder;
		this.nestedParser = nestedParser;
	}

	@Override
	public TDAParsing tryParsing(Tokenizable toks) {
		nestedParser.anotherParent();
		ExprToken tok = ExprToken.from(errors, toks);
		if ("<-".equals(tok.text))
			return handleSend(tok.location, toks);
		else
			return handleAssign(tok, toks);
	}

	private TDAParsing handleSend(InputPosition arrowPos, Tokenizable toks) {
		List<Expr> seen = new ArrayList<>();
		new TDAExpressionParser(errors, t -> {
			seen.add(t);
			SendMessage msg = new SendMessage(arrowPos, t);
			builder.sendMessage(msg);
		}).tryParsing(toks);
		if (seen.isEmpty()) {
			errors.message(toks, "no expression to send");
			return new IgnoreNestedParser();
		}
		return nestedParser;
	}

	private TDAParsing handleAssign(ExprToken tok, Tokenizable toks) {
		List<UnresolvedVar> slots = new ArrayList<>();
		boolean haveDot = true;
		while (tok != null && tok.type == ExprToken.IDENTIFIER) {
			haveDot = false;
			UnresolvedVar v = new UnresolvedVar(tok.location, tok.text);
			slots.add(v);
			tok = ExprToken.from(errors, toks);
			if (tok == null) {
				errors.message(toks, "syntax error");
				return new IgnoreNestedParser();
			}
			if (tok.type == ExprToken.PUNC && ".".equals(tok.text)) {
				tok = ExprToken.from(errors, toks);
				haveDot = true;
			} else
				break;
		}
		if (tok == null) {
			errors.message(toks, "syntax error");
			return new IgnoreNestedParser();
		}
		if ("<-".equals(tok.text)) {
			InputPosition pos = tok.location;
			List<Expr> seen = new ArrayList<>();
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
		return nestedParser;
	}

	@Override
	public void scopeComplete(InputPosition location) {
		builder.done();
	}
}
