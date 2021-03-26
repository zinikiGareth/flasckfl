package org.flasck.flas.parser.assembly;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.parser.IgnoreNestedParser;
import org.flasck.flas.parser.NoNestingParser;
import org.flasck.flas.parser.TDAExpressionParser;
import org.flasck.flas.parser.TDAParsing;
import org.flasck.flas.tokenizers.ExprToken;
import org.flasck.flas.tokenizers.Tokenizable;
import org.flasck.flas.tokenizers.TypeNameToken;
import org.flasck.flas.tokenizers.ValidIdentifierToken;
import org.flasck.flas.tokenizers.VarNameToken;

public class TDAEnterExitParser implements TDAParsing {
	private final ErrorReporter errors;
	private final RoutingActionConsumer consumer;

	public TDAEnterExitParser(ErrorReporter errors, RoutingActionConsumer consumer) {
		this.errors = errors;
		this.consumer = consumer;
	}

	@Override
	public TDAParsing tryParsing(Tokenizable toks) {
		ValidIdentifierToken cardToken = VarNameToken.from(toks);
		if (cardToken == null) {
			errors.message(toks, "expected card reference");
			return new NoNestingParser(errors);
		}
		UnresolvedVar card = new UnresolvedVar(cardToken.location, cardToken.text);
		
		ExprToken tok = ExprToken.from(errors, toks);
		if (tok == null) {
			errors.message(toks, "expected . or <-");
			return new NoNestingParser(errors);
		}
		
		switch (tok.text) {
		case ".": {
			ValidIdentifierToken meth = VarNameToken.from(toks);
			if (meth == null) {
				errors.message(toks, "expected method");
				return new NoNestingParser(errors);
			}
			switch (meth.text) {
			case "load":
			case "nest": {
				List<Expr> expr = new ArrayList<>();
				new TDAExpressionParser(errors, e -> {
					expr.add(e);
				}).tryParsing(toks);
				if (expr.size() == 0) {
					errors.message(toks, "syntax error");
					return new IgnoreNestedParser();
				} else if (expr.size() > 1) {
					errors.message(expr.get(1).location(), "syntax error");
					return new IgnoreNestedParser();
				}
				if ("load".equals(meth.text))
					consumer.load(card, expr.get(0));
				else
					consumer.next(card, expr.get(0));
				break;
			}
			case "done": {
				consumer.done(card);
				break;
			}
			}
			break;
		}
		case "<-": {
			TypeNameToken cardName = TypeNameToken.qualified(toks);
			if (cardName == null) {
				errors.message(toks, "card name required");
				return new IgnoreNestedParser();
			}
			consumer.assignCard(card, new TypeReference(cardName.location, cardName.text));
			break;
		}
		default: {
			errors.message(toks, "expected . or <-");
			return new NoNestingParser(errors);
		}
		}

		if (toks.hasMoreContent())
			errors.message(toks, "syntax error");

		return new NoNestingParser(errors);
	}

	@Override
	public void scopeComplete(InputPosition location) {
		// TODO Auto-generated method stub

	}

}
