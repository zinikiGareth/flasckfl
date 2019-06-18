package org.flasck.flas.parser.ut;

import java.util.function.Consumer;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parser.IgnoreNestedParser;
import org.flasck.flas.parser.NoNestingParser;
import org.flasck.flas.parser.TDAExpressionParser;
import org.flasck.flas.parser.TDAParsing;
import org.flasck.flas.tokenizers.Tokenizable;

public class SingleExpressionParser implements TDAParsing {
	private final ErrorReporter errors;
	private final Consumer<Expr> builder;
	private int exprCount = 0;

	public SingleExpressionParser(ErrorReporter errors, Consumer<Expr> builder) {
		this.errors = errors;
		this.builder = builder;
	}

	@Override
	public TDAParsing tryParsing(Tokenizable toks) {
		if (++exprCount > 1)
			return new IgnoreNestedParser();
		TDAExpressionParser expr = new TDAExpressionParser(errors, builder);
		expr.tryParsing(toks);
		if (toks.hasMore()){
			errors.message(toks, "syntax error");
			return new IgnoreNestedParser();
		}
		return new NoNestingParser(errors);
	}

	@Override
	public void scopeComplete(InputPosition location) {
		if (exprCount != 1)
			errors.message(location, "assert requires exactly one match expression");
	}

}
