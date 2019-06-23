package org.flasck.flas.repository;

import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.parsedForm.FunctionCaseDefn;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.repository.Repository.Visitor;

public class Traverser {
	private final Visitor visitor;

	public Traverser(Visitor visitor) {
		this.visitor = visitor;
		// TODO Auto-generated constructor stub
	}

	public void process(RepositoryEntry e) {
		if (e == null)
			throw new org.zinutils.exceptions.NotImplementedException("traverser cannot handle null entries");
		else if (e instanceof FunctionDefinition)
			handleFunction((FunctionDefinition)e);
		else
			throw new org.zinutils.exceptions.NotImplementedException("traverser cannot handle " + e.getClass());
	}

	private void handleFunction(FunctionDefinition e) {
		for (FunctionIntro i : e.intros())
			handleIntro(i);
	}

	private void handleIntro(FunctionIntro i) {
		// TODO: process args
		for (FunctionCaseDefn c : i.cases())
			handleFnCase(c);
	}

	private void handleFnCase(FunctionCaseDefn c) {
		if (c.guard != null)
			processExpr(c.guard);
		processExpr(c.expr);
	}

	private void processExpr(Expr e) {
		if (e == null)
			throw new org.zinutils.exceptions.NotImplementedException("traverser cannot handle null expr");
		else if (e instanceof UnresolvedVar)
			visitor.visitUnresolvedVar((UnresolvedVar) e);
		else
			throw new org.zinutils.exceptions.NotImplementedException("traverser cannot handle expr " + e.getClass());
	}

}
