package org.flasck.flas.parser.ut;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.parser.IgnoreNestedParser;
import org.flasck.flas.parser.NoNestingParser;
import org.flasck.flas.parser.TDAExpressionParser;
import org.flasck.flas.parser.TDAParsing;
import org.flasck.flas.tokenizers.ExprToken;
import org.flasck.flas.tokenizers.Tokenizable;
import org.flasck.flas.tokenizers.ValidIdentifierToken;
import org.flasck.flas.tokenizers.VarNameToken;

public class TDAProcessFieldsParser implements TDAParsing {
	private final ErrorReporter errors;
	private final UnitDataFieldConsumer data;

	public TDAProcessFieldsParser(ErrorReporter errors, UnitDataFieldConsumer data) {
		this.errors = errors;
		this.data = data;
	}

	@Override
	public TDAParsing tryParsing(Tokenizable toks) {
		ValidIdentifierToken var = VarNameToken.from(errors, toks);
		if (var == null) {
			errors.message(toks, "syntax error");
			return new IgnoreNestedParser();
		}
		ExprToken send = ExprToken.from(errors, toks);
		if (send == null) {
			errors.message(toks, "expected <-");
			return new IgnoreNestedParser();
		} else if (!send.text.equals("<-")) {
			errors.message(send.location, "expected <-");
			return new IgnoreNestedParser();
		}
		List<Expr> exprs = new ArrayList<>();
		new TDAExpressionParser(errors, x->exprs.add(x)).tryParsing(toks);
		if (exprs.isEmpty()) {
			// it failed
			return new IgnoreNestedParser();
		}
		if (toks.hasMoreContent()) {
			errors.message(toks, "syntax error");
			return new IgnoreNestedParser();
		}
		data.field(new UnresolvedVar(var.location, var.text), exprs.get(0));
		return new NoNestingParser(errors);
	}

	@Override
	public void scopeComplete(InputPosition location) {
	}

}
