package org.flasck.flas.parser;

import org.flasck.flas.commonBase.Expr;

public interface ExprTermConsumer {
	boolean isTop();
	void showStack(StackDumper d);
	void term(Expr term);
	void done();
}
