package org.flasck.flas.parser;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.commonBase.MemberExpr;
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
		else if (tok.type == ExprToken.IDENTIFIER)
			return handleAssign(tok, toks);
		else {
			errors.message(toks, "expected assign or send message");
			return new IgnoreNestedParser();
		}
			
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
		Expr slot = null;
		while (true) {
			UnresolvedVar var = new UnresolvedVar(tok.location, tok.text);
			if (slot == null) {
				slot = var;
			} else
				slot = new MemberExpr(tok.location, slot, var);
			// read . or <-
			tok = ExprToken.from(errors, toks);
			if (tok == null) {
				errors.message(toks, "expected . or <-");
				return new IgnoreNestedParser();
			}
			if (tok.type == ExprToken.PUNC && ".".equals(tok.text)) {
				// read another identifier
				tok = ExprToken.from(errors, toks);
				if (tok == null || tok.type != ExprToken.IDENTIFIER) {
					errors.message(toks, "expected identifier");
					return new IgnoreNestedParser();
				}
			} else
				break;
		}
		if (!"<-".equals(tok.text)) {
			errors.message(toks, "expected <-");
			return new IgnoreNestedParser();
		}
		InputPosition pos = tok.location;
		List<Expr> seen = new ArrayList<>();
		final Expr slotExpr = slot;
		new TDAExpressionParser(errors, t -> {
			seen.add(t);
			AssignMessage msg = new AssignMessage(pos, slotExpr, t);
			builder.assignMessage(msg);
		}).tryParsing(toks);
		if (seen.isEmpty()) {
			errors.message(toks, "no expression to send");
			return new IgnoreNestedParser();
		}
		return nestedParser;
	}

	@Override
	public void scopeComplete(InputPosition location) {
		builder.done();
	}
}
