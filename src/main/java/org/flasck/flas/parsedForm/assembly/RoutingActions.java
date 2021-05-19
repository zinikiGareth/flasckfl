package org.flasck.flas.parsedForm.assembly;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.parser.assembly.RoutingActionConsumer;

public class RoutingActions implements RoutingActionConsumer {
	private final InputPosition loc;
	public final List<RoutingAction> actions = new ArrayList<>();

	public RoutingActions(InputPosition loc) {
		this.loc = loc;
	}

	public InputPosition location() {
		return loc;
	}

	@Override
	public void method(UnresolvedVar card, TypeReference contract, String meth, List<Expr> exprs) {
		actions.add(new RoutingAction(card, contract, meth, exprs));
	}
}
