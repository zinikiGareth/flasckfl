package org.flasck.flas.parser;

import org.flasck.flas.commonBase.Expr;

public interface ExprTokenConsumer {

	void term(Expr term);

	void done();

}
