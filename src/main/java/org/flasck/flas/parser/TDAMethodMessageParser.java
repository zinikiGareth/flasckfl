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

public class TDAMethodMessageParser extends BlockLocationTracker implements TDAParsing {
	protected final MethodMessagesConsumer builder;
	protected final LastOneOnlyNestedParser nestedParser;

	public TDAMethodMessageParser(ErrorReporter errors, MethodMessagesConsumer builder, LastOneOnlyNestedParser nestedParser, LocationTracker locTracker) {
		super(errors, locTracker);
		this.builder = builder;
		this.nestedParser = nestedParser;
	}

	@Override
	public TDAParsing tryParsing(Tokenizable toks) {
		nestedParser.anotherParent();
		ExprToken tok = ExprToken.from(errors, toks);
		updateLoc(tok.location);
		if ("<-".equals(tok.text))
			return handleSend(tok.location, toks);
		else if (tok.type == ExprToken.IDENTIFIER)
			return handleAssign(tok, toks);
		else {
			errors.message(toks, "expected assign or send message");
			return new IgnoreNestedParser(errors);
		}
			
	}

	private TDAParsing handleSend(InputPosition arrowPos, Tokenizable toks) {
		List<SendMessage> seen = new ArrayList<>();
		new TDAExpressionParser(errors, t -> {
			SendMessage msg = new SendMessage(arrowPos, t);
			seen.add(msg);
			builder.sendMessage(msg);
		}).tryParsing(toks);
		if (seen.isEmpty()) {
			errors.message(toks, "no expression to send");
			return new IgnoreNestedParser(errors);
		} else if (toks.hasMoreContent(errors)) {
			ExprToken tok = ExprToken.from(errors, toks);
			if (tok.type == ExprToken.SYMBOL && tok.text.equals("=>")) {
				new TDAExpressionParser(errors, t -> {
					seen.get(0).handlerNameExpr(t);
				}).tryParsing(toks);
				if (toks.hasMoreContent(errors)) {
					errors.message(toks, "syntax error");
					return new IgnoreNestedParser(errors);
				}
				errors.logReduction("method-message-send-handled", arrowPos, toks.realinfo());
			} else {
				errors.message(toks, "syntax error");
				return new IgnoreNestedParser(errors);
			}
		} else {
			errors.logReduction("method-message-send", arrowPos, toks.realinfo());
		}
		tellParent(arrowPos);
		return nestedParser;
	}

	private TDAParsing handleAssign(ExprToken tok, Tokenizable toks) {
		Expr slot = null;
		while (true) {
			UnresolvedVar var = new UnresolvedVar(tok.location, tok.text);
			if (slot == null) {
				slot = var;
			} else
				slot = new MemberExpr(slot.location(), slot, var);
			// read . or <-
			tok = ExprToken.from(errors, toks);
			if (tok == null) {
				errors.message(toks, "expected . or <-");
				return new IgnoreNestedParser(errors);
			}
			if (tok.type == ExprToken.PUNC && ".".equals(tok.text)) {
				// read another identifier
				tok = ExprToken.from(errors, toks);
				if (tok == null || tok.type != ExprToken.IDENTIFIER) {
					errors.message(toks, "expected identifier");
					return new IgnoreNestedParser(errors);
				}
			} else
				break;
		}
		if (!"<-".equals(tok.text)) {
			errors.message(tok.location, "expected <-");
			return new IgnoreNestedParser(errors);
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
			return new IgnoreNestedParser(errors);
		}
		errors.logReduction("assign-method-action", slot, seen.get(seen.size()-1));
		tellParent(slot.location());
		return nestedParser;
	}

	@Override
	public void scopeComplete(InputPosition location) {
		builder.done();
	}
}
