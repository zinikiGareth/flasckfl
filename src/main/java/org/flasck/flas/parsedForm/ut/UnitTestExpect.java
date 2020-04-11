package org.flasck.flas.parsedForm.ut;

import java.util.Arrays;
import java.util.List;

import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.parsedForm.UnresolvedVar;

public class UnitTestExpect implements UnitTestStep {
	public final UnresolvedVar ctr;
	public final UnresolvedVar method;
	public final List<Expr> args;
	public final Expr handler;
	
	public UnitTestExpect(UnresolvedVar ctr, UnresolvedVar meth, Expr[] args, Expr handler) {
		this.ctr = ctr;
		this.method = meth;
		this.args = Arrays.asList(args);
		this.handler = handler;
	}
}
