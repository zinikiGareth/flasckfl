package org.flasck.flas.parser.ut;

import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.parsedForm.UnresolvedVar;

public interface UnitDataFieldConsumer {

	void field(UnresolvedVar field, Expr value);

}
