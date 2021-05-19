package org.flasck.flas.parser.assembly;

import java.util.List;

import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.parsedForm.UnresolvedVar;

public interface RoutingActionConsumer {
	void method(UnresolvedVar card, TypeReference contract, String meth, List<Expr> exprs);
}
