package org.flasck.flas.parsedForm.assembly;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Expr;
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
	public void load(UnresolvedVar card, Expr expr) {
		actions.add(new RoutingAction("load", card, expr));
	}

	@Override
	public void nest(UnresolvedVar card, Expr expr) {
		actions.add(new RoutingAction("nest", card, expr));
	}

	@Override
	public void done(UnresolvedVar card) {
		actions.add(new RoutingAction("done", card, null));
	}

}
