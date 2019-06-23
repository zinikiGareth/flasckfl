package org.flasck.flas.repository;

import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.parsedForm.FunctionCaseDefn;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.parsedForm.UnresolvedOperator;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.repository.Repository.Visitor;

/** The purpose of the leaf adapter is to visit all of the leaves in the system.
 *
 *  This is not suitable for things that want to see (for example) 'apply' or 'dot' nodes in expressions, which should use the Pre or Post adapters 
 */
public class LeafAdapter implements Visitor {
	@Override
	public void visitFunction(FunctionDefinition fn) {
		for (FunctionIntro i : fn.intros())
			visitIntro(i);
	}

	@Override
	public void visitIntro(FunctionIntro i) {
		for (FunctionCaseDefn c : i.cases())
			visitCase(c);
	}

	@Override
	public void visitCase(FunctionCaseDefn c) {
		// TODO: visit args
		// TODO: visit guard
		visitExpr(c.expr);
	}

	public void visitExpr(Expr expr) {
		if (expr instanceof UnresolvedVar)
			visitUnresolvedVar((UnresolvedVar) expr);
		else
			throw new org.zinutils.exceptions.NotImplementedException("Not handled: " + expr.getClass());
	}

	@Override
	public void visitUnresolvedVar(UnresolvedVar var) {
	}

	@Override
	public void visitUnresolvedOperator(UnresolvedOperator operator) {
	}

	@Override
	public void visitTypeReference(TypeReference var) {
	}
}
