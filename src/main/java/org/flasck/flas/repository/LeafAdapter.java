package org.flasck.flas.repository;

import org.flasck.flas.commonBase.ApplyExpr;
import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.commonBase.NumericLiteral;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.parsedForm.FunctionCaseDefn;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.StructField;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.parsedForm.TypedPattern;
import org.flasck.flas.parsedForm.UnresolvedOperator;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.parsedForm.ut.UnitTestCase;
import org.flasck.flas.repository.Repository.Visitor;

/** The purpose of the leaf adapter is to visit all of the leaves in the system.
 *
 *  This is not suitable for things that want to see (for example) 'apply' or 'dot' nodes in expressions, which should use the Pre or Post adapters 
 */
public class LeafAdapter implements Visitor {
	@Override
	public void visitEntry(RepositoryEntry entry) {
	}

	@Override
	public void visitStructDefn(StructDefn s) {
	}

	@Override
	public void visitStructField(StructField sf) {
	}

	@Override
	public void visitFunction(FunctionDefinition fn) {
	}

	@Override
	public void leaveFunction(FunctionDefinition fn) {
	}

	@Override
	public void visitIntro(FunctionIntro i) {
	}

	@Override
	public void visitPattern(Object patt) {
	}

	@Override
	public void visitTypedPattern(TypedPattern p) {
	}

	@Override
	public void visitCase(FunctionCaseDefn c) {
	}

	@Override
	public void visitExpr(Expr expr) {
	}

	@Override
	public void visitApplyExpr(ApplyExpr expr) {
	}

	@Override
	public void visitStringLiteral(StringLiteral expr) {
	}

	@Override
	public void visitNumericLiteral(NumericLiteral number) {
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

	@Override
	public void visitUnitTest(UnitTestCase e) {
	}
}
