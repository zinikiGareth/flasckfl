package org.flasck.flas.parsedForm.st;

import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.parsedForm.IntroduceVar;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.parsedForm.ut.UnitTestStep;

public class GotoRoute implements UnitTestStep {
	public final Expr route;
	public final IntroduceVar iv;
	public final UnresolvedVar app;

	public GotoRoute(UnresolvedVar app, Expr route, IntroduceVar iv) {
		this.app = app;
		this.route = route;
		this.iv = iv;
	}
}
