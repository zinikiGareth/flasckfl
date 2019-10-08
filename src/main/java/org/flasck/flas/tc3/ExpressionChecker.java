package org.flasck.flas.tc3;

import org.flasck.flas.commonBase.ApplyExpr;
import org.flasck.flas.commonBase.NumericLiteral;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.TypedPattern;
import org.flasck.flas.parsedForm.UnresolvedOperator;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.parsedForm.VarPattern;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.repository.RepositoryReader;
import org.flasck.flas.repository.ResultAware;

public class ExpressionChecker extends LeafAdapter implements ResultAware {
	private final RepositoryReader r;
	private final NestedVisitor nv;
	private final CurrentTCState state;
	private final ErrorReporter errors;

	public ExpressionChecker(ErrorReporter errors, RepositoryReader repository, CurrentTCState state, NestedVisitor nv) {
		this.errors = errors;
		this.r = repository;
		this.state = state;
		this.nv = nv;
	}
	
	@Override
	public void visitNumericLiteral(NumericLiteral number) {
		nv.result(r.get("Number"));
	}

	@Override
	public void visitStringLiteral(StringLiteral s) {
		nv.result(r.get("String"));
	}

	@Override
	public void visitUnresolvedVar(UnresolvedVar var, int nargs) {
		if (var == null || var.defn() == null)
			throw new NullPointerException("undefined var: " + var);
		if (var.defn() instanceof StructDefn) {
			nv.result(var.defn());
		} else if (var.defn() instanceof FunctionDefinition) {
			FunctionDefinition fn = (FunctionDefinition) var.defn();
			nv.result(fn.type());
		} else if (var.defn() instanceof VarPattern) {
			VarPattern vp = (VarPattern) var.defn();
			nv.result(state.functionParameter(vp.location(), vp.name().uniqueName()));
		} else if (var.defn() instanceof TypedPattern) {
			TypedPattern vp = (TypedPattern) var.defn();
			nv.result(vp.type.defn());
		} else
			throw new RuntimeException("Cannot handle " + var.defn() + " of type " + var.defn().getClass());
	}
	
	@Override
	public void visitUnresolvedOperator(UnresolvedOperator var, int nargs) {
		if (var.defn() instanceof StructDefn) {
			nv.result(var.defn());
		} else if (var.defn() instanceof FunctionDefinition) {
			FunctionDefinition fn = (FunctionDefinition) var.defn();
			nv.result(fn.type());
		} else
			throw new RuntimeException("Cannot handle " + var);
	}
	
	@Override
	public void visitApplyExpr(ApplyExpr expr) {
		nv.push(new ApplyExpressionChecker(errors, r, state, nv));
	}
	
	@Override
	public void result(Object r) {
		nv.result(r);
	}
}
