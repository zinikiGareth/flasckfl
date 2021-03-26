package org.flasck.flas.parsedForm.assembly;

import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.parser.assembly.MainRoutingActionConsumer;
import org.flasck.flas.parser.assembly.RoutingActionConsumer;

public class SubRouting implements RoutingActionConsumer {
	private final MainRoutingActionConsumer main;

	public SubRouting(MainRoutingActionConsumer main) {
		if (main == null)
			this.main = (MainRoutingActionConsumer) this;
		else
			this.main = main;
	}
	
	@Override
	public void load(UnresolvedVar card, Expr expr) {
		// TODO Auto-generated method stub

	}

	@Override
	public void next(UnresolvedVar card, Expr expr) {
		// TODO Auto-generated method stub

	}

	@Override
	public void assignCard(UnresolvedVar var, TypeReference cardType) {
		main.nameCard(var, cardType);
	}

	@Override
	public void done(UnresolvedVar card) {
		// TODO Auto-generated method stub
		
	}
}
