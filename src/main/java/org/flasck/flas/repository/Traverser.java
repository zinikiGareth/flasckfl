package org.flasck.flas.repository;

import org.flasck.flas.commonBase.ApplyExpr;
import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.commonBase.NumericLiteral;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.parsedForm.ContractDecl;
import org.flasck.flas.parsedForm.ContractMethodDecl;
import org.flasck.flas.parsedForm.FunctionCaseDefn;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.parsedForm.ObjectDefn;
import org.flasck.flas.parsedForm.ObjectMethod;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.StructField;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.parsedForm.TypedPattern;
import org.flasck.flas.parsedForm.UnresolvedOperator;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.parsedForm.ut.UnitTestAssert;
import org.flasck.flas.parsedForm.ut.UnitTestCase;
import org.flasck.flas.parsedForm.ut.UnitTestStep;
import org.flasck.flas.repository.Repository.Visitor;
import org.zinutils.exceptions.NotImplementedException;

public class Traverser implements Visitor {
	private final Visitor visitor;

	public Traverser(Visitor visitor) {
		this.visitor = visitor;
	}

	/** It's starting to concern me that for some things (contracts, unit tests) we visit
	 * the parent object and then all of its children, but for other things,
	 * such as objects and their methods, we view both as being in the repository and allow
	 * the repository to do its work in any order.
	 */
	@Override
	public void visitEntry(RepositoryEntry e) {
		if (e == null)
			throw new org.zinutils.exceptions.NotImplementedException("traverser cannot handle null entries");
		else if (e instanceof BuiltinRepositoryEntry)
			; // do nothing for builtins
		else if (e instanceof ContractDecl)
			visitContractDecl((ContractDecl)e);
		else if (e instanceof ObjectDefn)
			visitObjectDefn((ObjectDefn)e);
		else if (e instanceof FunctionDefinition)
			visitFunction((FunctionDefinition)e);
		else if (e instanceof ObjectMethod)
			visitObjectMethod((ObjectMethod)e);
		else if (e instanceof StructDefn)
			visitStructDefn((StructDefn)e);
		else if (e instanceof UnitTestCase)
			visitUnitTest((UnitTestCase)e);
		else
			throw new org.zinutils.exceptions.NotImplementedException("traverser cannot handle " + e.getClass());
	}

	@Override
	public void visitStructDefn(StructDefn s) {
		visitor.visitStructDefn(s);
		for (StructField f : s.fields)
			visitStructField(f);
		leaveStructDefn(s);
	}
	
	@Override
	public void visitStructField(StructField sf) {
		visitor.visitStructField(sf);
	}

	@Override
	public void leaveStructDefn(StructDefn s) {
		visitor.leaveStructDefn(s);
	}

	@Override
	public void visitObjectDefn(ObjectDefn e) {
		visitor.visitObjectDefn(e);
	}

	@Override
	public void visitObjectMethod(ObjectMethod e) {
		visitor.visitObjectMethod(e);
	}

	@Override
	public void visitFunction(FunctionDefinition fn) {
		visitor.visitFunction(fn);
		for (FunctionIntro i : fn.intros())
			visitIntro(i);
		visitor.leaveFunction(fn);
	}

	@Override
	public void leaveFunction(FunctionDefinition fn) {
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
		else if (expr instanceof ApplyExpr)
			visitApplyExpr((ApplyExpr)expr);
		else if (expr instanceof StringLiteral)
			visitStringLiteral((StringLiteral)expr);
		else if (expr instanceof NumericLiteral)
			visitNumericLiteral((NumericLiteral)expr);
		else if (expr instanceof UnresolvedVar)
			visitUnresolvedVar((UnresolvedVar) expr);
		else
			throw new org.zinutils.exceptions.NotImplementedException("Not handled: " + expr.getClass());
	}

	public void visitApplyExpr(ApplyExpr expr) {
		visitor.visitApplyExpr(expr);
		visitExpr((Expr) expr.fn);
		for (Object x : expr.args)
			visitExpr((Expr) x);
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

	@Override
	public void visitNumericLiteral(NumericLiteral expr) {
		visitor.visitNumericLiteral(expr);
	}

	@Override
	public void visitUnitTest(UnitTestCase e) {
		visitor.visitUnitTest(e);
		for (UnitTestStep s : e.steps) {
			visitUnitTestStep(s);
		}
		visitor.leaveUnitTest(e);
	}

	@Override
	public void leaveUnitTest(UnitTestCase e) {
	}

	@Override
	public void visitUnitTestStep(UnitTestStep s) {
		visitor.visitUnitTestStep(s);
		if (s instanceof UnitTestAssert)
			visitUnitTestAssert((UnitTestAssert) s);
		else
			throw new NotImplementedException();
	}

	@Override
	public void visitUnitTestAssert(UnitTestAssert a) {
		visitor.visitUnitTestAssert(a);
		visitExpr(a.expr);
		visitExpr(a.value);
		visitor.postUnitTestAssert(a);
	}

	@Override
	public void postUnitTestAssert(UnitTestAssert a) {
	}

	@Override
	public void visitContractDecl(ContractDecl cd) {
		visitor.visitContractDecl(cd);
		for (ContractMethodDecl m : cd.methods)
			visitContractMethod(m);
		visitor.leaveContractDecl(cd);
	}

	@Override
	public void visitContractMethod(ContractMethodDecl cmd) {
		visitor.visitContractMethod(cmd);
	}

	@Override
	public void leaveContractDecl(ContractDecl cd) {
	}
}
