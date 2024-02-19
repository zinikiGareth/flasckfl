package org.flasck.flas.parser;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.blocker.TDAParsingWithAction;
import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.commonBase.Locatable;
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
	protected final BlockLocationTracker actionsTracker;

	public TDAMethodMessageParser(ErrorReporter errors, MethodMessagesConsumer builder, LastOneOnlyNestedParser nestedParser, LocationTracker locTracker, BlockLocationTracker actionsTracker) {
		super(errors, locTracker);
		this.builder = builder;
		this.nestedParser = nestedParser;
		this.actionsTracker = actionsTracker;
	}

	@Override
	public TDAParsing tryParsing(Tokenizable toks) {
		nestedParser.anotherParent();
		nestedParser.bindLocationTracker(this);
		ExprToken tok = ExprToken.from(errors, toks);
		updateLoc(tok.location);
		if (actionsTracker != null && actionsTracker.lastInner() == null)
			actionsTracker.updateLoc(tok.location);
		if ("<-".equals(tok.text))
			return handleSend(tok, toks);
		else if (tok.type == ExprToken.IDENTIFIER)
			return handleAssign(tok, toks);
		else {
			errors.message(toks, "expected assign or send message");
			return new IgnoreNestedParser(errors);
		}
	}

	private TDAParsing handleSend(Locatable arrowPos, Tokenizable toks) {
		List<SendMessage> seen = new ArrayList<>();
		Tokenizable ec = toks.copyTo("->");
		new TDAExpressionParser(errors, t -> {
			SendMessage msg = new SendMessage(arrowPos.location(), t);
			seen.add(msg);
		}).tryParsing(ec);
		if (seen.isEmpty()) {
			errors.message(toks, "no expression to send");
			return new IgnoreNestedParser(errors);
		}
		
		SendMessage send = seen.get(0);
		Locatable last = send.expr;

		if (ec != toks) { // handler case
			String handled = "";
			toks.reset(ec.at());
			ExprToken htok = ExprToken.from(errors, toks);
			if (htok.text.equals("->")) {
				handled = "-handled";
				Tokenizable handlerText = toks.copyTo("=>");
				TDAParsing rp = new TDAExpressionParser(errors, t -> {
					send.handlerExpr(t);
				}).tryParsing(handlerText);
				if (send.handlerExpr() == null) {
					errors.message(htok.location, "no valid handler specified");
					return rp;
				}
				last = send.handlerExpr();
				errors.logReduction("maybe-handled", htok, seen.get(0).handlerExpr());
				toks.reset(handlerText.at());
			} else {
				toks.reset(ec.at());
			}
			if (toks.hasMoreContent(errors)) {
				ExprToken tok = ExprToken.from(errors, toks);
				if (tok.type == ExprToken.SYMBOL && tok.text.equals("=>")) {
					new TDAExpressionParser(errors, t -> {
						send.subscriberNameExpr(t);
					}).tryParsing(toks);
					if (toks.hasMoreContent(errors)) {
						errors.message(toks, "syntax error");
						return new IgnoreNestedParser(errors);
					}
					if (send.subscriberName() == null) {
						errors.message(htok.location, "no valid subscription specified");
						return new IgnoreNestedParser(errors);
					}
					errors.logReduction("maybe-subscribed", tok, send.subscriberName());
					errors.logReduction("method-message-send" + handled +"-subscribed", arrowPos, tok);
				} else {
					errors.message(toks, "syntax error");
					return new IgnoreNestedParser(errors);
				}
			} else {
				errors.logReduction("method-message-send" + handled, arrowPos, last);
			}
		} else {
			errors.logReduction("method-message-send", arrowPos, last);
		}
		builder.sendMessage(send);
		tellParent(arrowPos.location());
		return new TDAParsingWithAction(nestedParser, reduceToActions(arrowPos));
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
		errors.logReduction("member-path", slot.location(), slot.location());
		errors.logReduction("assign-method-action", slot, seen.get(seen.size()-1));
		tellParent(slot.location());
		return new TDAParsingWithAction(nestedParser, reduceToActions(slot));
	}

	private Runnable reduceToActions(Locatable start) {
		return () -> {
			if (lastInner().compareTo(start.location()) > 0) {
				reduce(actionsTracker.lastInner(), "method-actions");
			}
		};
	}

	@Override
	public void scopeComplete(InputPosition location) {
		builder.done();
	}
}
