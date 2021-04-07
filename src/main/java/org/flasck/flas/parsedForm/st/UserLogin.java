package org.flasck.flas.parsedForm.st;

import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.parsedForm.ut.UnitTestStep;

public class UserLogin implements UnitTestStep {
	public final UnresolvedVar app;
	public final Expr user;

	public UserLogin(UnresolvedVar app, Expr user) {
		this.app = app;
		this.user = user;
	}
}
