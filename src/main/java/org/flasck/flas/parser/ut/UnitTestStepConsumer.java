package org.flasck.flas.parser.ut;

import org.flasck.flas.commonBase.Expr;

public interface UnitTestStepConsumer {

	void assertion(Expr expr, Expr value);

}
