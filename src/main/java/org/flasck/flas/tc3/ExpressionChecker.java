package org.flasck.flas.tc3;

import org.flasck.flas.commonBase.NumericLiteral;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.repository.RepositoryReader;

public class ExpressionChecker extends LeafAdapter {
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
		} else
			throw new RuntimeException("Cannot handle " + var);
	}
}
