package org.flasck.flas.parser.assembly;

import java.util.List;

import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.parsedForm.UnresolvedVar;

public interface RoutingActionConsumer {
	void method(UnresolvedVar card, String meth, List<Expr> exprs);
}
