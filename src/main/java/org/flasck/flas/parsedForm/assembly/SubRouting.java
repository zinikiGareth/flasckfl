package org.flasck.flas.parsedForm.assembly;

import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.parser.assembly.MainRoutingGroupConsumer;
import org.flasck.flas.parser.assembly.RoutingActionConsumer;

public class SubRouting implements RoutingActionConsumer {
	protected final MainRoutingGroupConsumer main;

	public SubRouting(MainRoutingGroupConsumer main) {
		if (main == null)
			this.main = (MainRoutingGroupConsumer) this;
		else
			this.main = main;
	}
	
	@Override
	public void load(UnresolvedVar card, Expr expr) {
		// TODO Auto-generated method stub
System.out.println("hello");
	}

	@Override
	public void next(UnresolvedVar card, Expr expr) {
		// TODO Auto-generated method stub

	}

	@Override
	public void done(UnresolvedVar card) {
		// TODO Auto-generated method stub
		
	}
}
