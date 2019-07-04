package org.flasck.flas.repository;

import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.parsedForm.FunctionCaseDefn;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.parsedForm.TypedPattern;
import org.flasck.flas.parsedForm.UnresolvedOperator;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.repository.Repository.Visitor;

public class Traverser implements Visitor {
	private final Visitor visitor;

	public Traverser(Visitor visitor) {
		this.visitor = visitor;
	}

	@Override
	public void visitEntry(RepositoryEntry e) {
		if (e == null)
			throw new org.zinutils.exceptions.NotImplementedException("traverser cannot handle null entries");
		else if (e instanceof FunctionDefinition)
			visitFunction((FunctionDefinition)e);
		else
			throw new org.zinutils.exceptions.NotImplementedException("traverser cannot handle " + e.getClass());
	}

	@Override
	public void visitFunction(FunctionDefinition fn) {
		visitor.visitFunction(fn);
		for (FunctionIntro i : fn.intros())
			visitIntro(i);
		this.leaveFunction(fn);
	}

	@Override
	public void leaveFunction(FunctionDefinition fn) {
		visitor.leaveFunction(fn);
	}

	@Override
	public void visitIntro(FunctionIntro i) {
		for (Object p : i.args)
			visitPattern(p);
		for (FunctionCaseDefn c : i.cases())
			visitCase(c);
	}

	@Override
	public void visitPattern(Object p) {
		if (p instanceof TypedPattern)
			visitTypedPattern((TypedPattern)p);
		else
			throw new org.zinutils.exceptions.NotImplementedException("Pattern not handled: " + p.getClass());
	}

	@Override
	public void visitTypedPattern(TypedPattern p) {
		visitTypeReference(p.type);
		// TODO: visitPatternVar(p.var);
	}

	@Override
	public void visitCase(FunctionCaseDefn c) {
		if (c.guard != null)
			visitExpr(c.guard);
		visitExpr(c.expr);
	}

	@Override
	public void visitExpr(Expr expr) {
		if (expr == null)
			return;
		else if (expr instanceof StringLiteral)
			visitStringLiteral((StringLiteral)expr);
		else if (expr instanceof UnresolvedVar)
			visitUnresolvedVar((UnresolvedVar) expr);
		else
			throw new org.zinutils.exceptions.NotImplementedException("Not handled: " + expr.getClass());
	}

	@Override
	public void visitUnresolvedVar(UnresolvedVar var) {
		visitor.visitUnresolvedVar(var);
	}

	@Override
	public void visitUnresolvedOperator(UnresolvedOperator operator) {
		visitor.visitUnresolvedOperator(operator);
	}

	@Override
	public void visitTypeReference(TypeReference var) {
		visitor.visitTypeReference(var);
	}

	@Override
	public void visitStringLiteral(StringLiteral expr) {
		visitor.visitStringLiteral(expr);
	}
}
