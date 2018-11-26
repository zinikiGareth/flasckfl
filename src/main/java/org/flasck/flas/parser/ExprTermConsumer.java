package org.flasck.flas.parser;

import org.flasck.flas.commonBase.Expr;

public interface ExprTermConsumer {
	void term(Expr term);
	void done();
}
