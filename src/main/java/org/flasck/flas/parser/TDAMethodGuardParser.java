package org.flasck.flas.parser;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.GuardedMessagesConsumer;
import org.flasck.flas.parsedForm.ut.GuardedMessages;
import org.flasck.flas.tokenizers.ExprToken;
import org.flasck.flas.tokenizers.Tokenizable;

public class TDAMethodGuardParser extends TDAMethodMessageParser implements TDAParsing {
	enum Mode { FIRST, WANTGUARDS, WANTMESSAGES };
	private Mode mode = Mode.FIRST;
	private final GuardedMessagesConsumer consumer;
	private boolean firstGuard = true;
	private boolean seenDefault = false;

	public TDAMethodGuardParser(ErrorReporter errors, MethodMessagesConsumer builder, LastOneOnlyNestedParser nestedParser) {
		super(errors, builder, nestedParser);
		consumer = (GuardedMessagesConsumer) builder;
	}

	@Override
	public TDAParsing tryParsing(Tokenizable toks) {
		switch (mode) {
		case WANTGUARDS:
			return parseGuard(toks);
		case WANTMESSAGES:
			return super.tryParsing(toks);
		default: {
			if (toks.charAt(0) == '|') {
				mode = Mode.WANTGUARDS;
				return parseGuard(toks);
			} else {
				mode = Mode.WANTMESSAGES;
				return super.tryParsing(toks);
			}
		}
		}
	}

	private TDAParsing parseGuard(Tokenizable toks) {
		ExprToken tok = ExprToken.from(errors, toks);
		if (!("|".equals(tok.text))) {
			errors.message(tok.location, "guard expected");
			return new IgnoreNestedParser(errors);
		}
		if (!toks.hasMoreContent(errors)) { // it's a default
			if (firstGuard) {
				errors.message(tok.location, "first guard cannot be default");
				return new IgnoreNestedParser(errors);
			}
			if (seenDefault) {
				errors.message(tok.location, "cannot provide two default guards");
				return new IgnoreNestedParser(errors);
			}
			GuardedMessages dgm = new GuardedMessages(tok.location, null);
			consumer.guard(dgm);
			errors.logReduction("method-guard-default", tok.location, tok.location);
			seenDefault = true;
			return new TDAMethodMessageParser(errors, dgm, nestedParser);
		}
		List<GuardedMessages> seen = new ArrayList<>();
		new TDAExpressionParser(errors, t -> {
			GuardedMessages gm = new GuardedMessages(tok.location, t);
			seen.add(gm);
			consumer.guard(gm);
		}).tryParsing(toks);
		if (seen.isEmpty()) {
			errors.message(toks, "no guard expression");
			return new IgnoreNestedParser(errors);
		}
		firstGuard = false;
		errors.logReduction("method-guard-default", tok.location, seen.get(seen.size()-1).location());
		return new TDAMethodMessageParser(errors, seen.get(0), nestedParser);

	}

	@Override
	public void scopeComplete(InputPosition location) {
		builder.done();
	}
}
