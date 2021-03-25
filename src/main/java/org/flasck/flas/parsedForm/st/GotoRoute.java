package org.flasck.flas.parsedForm.st;

import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.parsedForm.IntroduceVar;
import org.flasck.flas.parsedForm.ut.UnitTestStep;

public class GotoRoute implements UnitTestStep {
	public final Expr route;
	public final IntroduceVar iv;

	public GotoRoute(Expr route, IntroduceVar iv) {
		this.route = route;
		this.iv = iv;
	}
}
