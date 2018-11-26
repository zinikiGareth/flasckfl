package org.flasck.flas.parser;

import org.flasck.flas.commonBase.Expr;

public interface ExprConsumer {

	void term(Expr unresolved);

}
