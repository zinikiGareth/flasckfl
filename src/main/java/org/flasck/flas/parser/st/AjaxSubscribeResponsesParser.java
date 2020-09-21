package org.flasck.flas.parser.st;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.ApplyExpr;
import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.errors.ErrorMark;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.UnresolvedOperator;
import org.flasck.flas.parsedForm.st.AjaxSubscribe;
import org.flasck.flas.parser.IgnoreNestedParser;
import org.flasck.flas.parser.NoNestingParser;
import org.flasck.flas.parser.TDAExpressionParser;
import org.flasck.flas.parser.TDAParsing;
import org.flasck.flas.tokenizers.Tokenizable;

public class AjaxSubscribeResponsesParser implements TDAParsing {
	private final ErrorReporter errors;
	private final AjaxSubscribe sub;

	public AjaxSubscribeResponsesParser(ErrorReporter errors, AjaxSubscribe sub) {
		this.errors = errors;
		this.sub = sub;
	}

	@Override
	public TDAParsing tryParsing(Tokenizable toks) {
		ErrorMark mark = errors.mark();
		List<Expr> es = new ArrayList<>();
		TDAExpressionParser ep = new TDAExpressionParser(errors, e -> es.add(e));
		ep.tryParsing(toks);
		if (mark.hasMoreNow()) {
			// we had errors, abort
			return new IgnoreNestedParser();
		}
		if (es.size() == 0) {
			toks.reset(0);
			errors.message(toks, "syntax error");
			return new IgnoreNestedParser();
		}
		if (es.size() > 1) {
			errors.message(es.get(1).location(), "only one response allowed per line");
			return new IgnoreNestedParser();
		}
		// TODO: This may in fact be too draconian
		// Arrays, numbers, etc are all fine
		// And function expressions that compute values should be fine, too ...
		// Possibly if we want this check, we want it to be more general and in the typechecker ...
		Expr expr = es.get(0);
		if (expr instanceof ApplyExpr) {
			ApplyExpr ae = (ApplyExpr) expr;
			if (ae.fn instanceof UnresolvedOperator) {
				UnresolvedOperator uo = (UnresolvedOperator)ae.fn;
				if (uo.op.equals("{}")) {
					sub.response(expr);
					return new NoNestingParser(errors);
				}
			}
		}
		errors.message(expr.location(), "response must be hash literal");
		return new IgnoreNestedParser();
	}

	@Override
	public void scopeComplete(InputPosition location) {
	}

}
