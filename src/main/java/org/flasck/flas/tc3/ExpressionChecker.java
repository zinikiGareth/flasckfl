package org.flasck.flas.tc3;

import org.flasck.flas.commonBase.ApplyExpr;
import org.flasck.flas.commonBase.NumericLiteral;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.UnresolvedOperator;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.repository.RepositoryReader;
import org.flasck.flas.repository.ResultAware;

public class ExpressionChecker extends LeafAdapter implements ResultAware {
	private final RepositoryReader r;
	private final NestedVisitor nv;

	public ExpressionChecker(RepositoryReader repository, NestedVisitor nv) {
		this.r = repository;
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
		if (var.defn() instanceof StructDefn) {
			nv.result(var.defn());
		} else if (var.defn() instanceof FunctionDefinition) {
			FunctionDefinition fn = (FunctionDefinition) var.defn();
			nv.result(fn.type());
		} else
			throw new RuntimeException("Cannot handle " + var);
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
		nv.push(new ApplyExpressionChecker(r, nv));
	}
	
	@Override
	public void result(Object r) {
		nv.result(r);
	}
}
