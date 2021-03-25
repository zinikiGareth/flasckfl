package org.flasck.flas.parser.assembly;

import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.parsedForm.UnresolvedVar;

public interface RoutingActionConsumer {

	void init(UnresolvedVar card, Expr expr);

	void next(UnresolvedVar card, Expr expr);

	void assignCard(UnresolvedVar var, TypeReference cardType);

	void done(UnresolvedVar card);
}
