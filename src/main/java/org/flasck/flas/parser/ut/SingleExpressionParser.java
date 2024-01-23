package org.flasck.flas.parser.ut;

import java.util.function.Consumer;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parser.IgnoreNestedParser;
import org.flasck.flas.parser.LocationTracker;
import org.flasck.flas.parser.NoNestingParser;
import org.flasck.flas.parser.TDAExpressionParser;
import org.flasck.flas.parser.TDAParsing;
import org.flasck.flas.tokenizers.Tokenizable;

public class SingleExpressionParser implements TDAParsing {
	private final ErrorReporter errors;
	private final String op;
	private final Consumer<Expr> builder;
	private final LocationTracker locTracker;
	private int exprCount = 0;
	private Expr exprLoc;

	public SingleExpressionParser(ErrorReporter errors, String op, Consumer<Expr> builder, LocationTracker locTracker) {
		this.errors = errors;
		this.op = op;
		this.builder = builder;
		this.locTracker = locTracker;
	}

	@Override
	public TDAParsing tryParsing(Tokenizable toks) {
		if (++exprCount > 1)
			return new IgnoreNestedParser(errors);
		TDAExpressionParser expr = new TDAExpressionParser(errors, e -> {
			exprLoc = e; 
			builder.accept(e);
		});
		expr.tryParsing(toks);
		if (toks.hasMoreContent(errors)){
			errors.message(toks, "syntax error");
			return new IgnoreNestedParser(errors);
		}
		locTracker.updateLoc(exprLoc.location());
		return new NoNestingParser(errors);
	}

	@Override
	public void scopeComplete(InputPosition location) {
		if (exprCount != 1)
			errors.message(location, op + " requires exactly one match expression");
	}

}
