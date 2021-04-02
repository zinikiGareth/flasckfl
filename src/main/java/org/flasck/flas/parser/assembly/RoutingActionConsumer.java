package org.flasck.flas.parser.assembly;

import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.parsedForm.UnresolvedVar;

public interface RoutingActionConsumer {
	void load(UnresolvedVar card, Expr expr);
	void next(UnresolvedVar card, Expr expr);
	void done(UnresolvedVar card);
}
