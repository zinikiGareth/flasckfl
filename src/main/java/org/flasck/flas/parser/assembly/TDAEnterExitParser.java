package org.flasck.flas.parser.assembly;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.ApplyExpr;
import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.parser.NoNestingParser;
import org.flasck.flas.parser.TDAExpressionParser;
import org.flasck.flas.parser.TDAParsing;
import org.flasck.flas.tokenizers.ExprToken;
import org.flasck.flas.tokenizers.Tokenizable;
import org.flasck.flas.tokenizers.TypeNameToken;
import org.flasck.flas.tokenizers.ValidIdentifierToken;
import org.flasck.flas.tokenizers.VarNameToken;
import org.zinutils.exceptions.NotImplementedException;

public class TDAEnterExitParser implements TDAParsing {
	private final ErrorReporter errors;
	private final RoutingActionConsumer consumer;

	public TDAEnterExitParser(ErrorReporter errors, RoutingActionConsumer consumer) {
		this.errors = errors;
		this.consumer = consumer;
	}

	@Override
	public TDAParsing tryParsing(Tokenizable toks) {
		ValidIdentifierToken cardToken = VarNameToken.from(errors, toks);
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
			TypeReference ctr;
			TypeNameToken tn = TypeNameToken.qualified(errors, toks);
			if (tn != null) {
				System.out.println(tn.text);
				tok = ExprToken.from(errors, toks);
				if (tok == null || !".".equals(tok.text)) {
					errors.message(toks, "expected .");
					return new NoNestingParser(errors);
				}
				ctr = new TypeReference(tn.location, tn.text, new ArrayList<>());
			} else {
				ctr = new TypeReference(card.location(), "Lifecycle", new ArrayList<>());
			}
			List<Expr> exprs = new ArrayList<>();
			new TDAExpressionParser(errors, e -> {
				exprs.add(e);
			}).tryParsing(toks);
			if (exprs.isEmpty()) {
				errors.message(toks, "expected method");
				return new NoNestingParser(errors);
			}
			Expr have = exprs.remove(0);
			UnresolvedVar uv;
			if (have instanceof UnresolvedVar)
				uv = (UnresolvedVar) have;
			else if (have instanceof ApplyExpr) {
				uv = (UnresolvedVar) ((ApplyExpr)have).fn;
				for (Object o : ((ApplyExpr)have).args)
					exprs.add((Expr) o);
			} else
				throw new NotImplementedException();
			consumer.method(card, ctr, uv.var, exprs);
			break;
		}
		default: {
			errors.message(toks, "expected . or <-");
			return new NoNestingParser(errors);
		}
		}

		if (toks.hasMoreContent(errors))
			errors.message(toks, "syntax error");

		return new NoNestingParser(errors);
	}

	@Override
	public void scopeComplete(InputPosition location) {
		// TODO Auto-generated method stub

	}

}
