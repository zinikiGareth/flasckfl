package org.flasck.flas.tc3;

import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.TypeExpr;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.LoadBuiltins;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.repository.RepositoryReader;

public class TypeExprChecker extends LeafAdapter {
	private final ErrorReporter errors;
	private final RepositoryReader repository;
	private final CurrentTCState state;
	private final NestedVisitor nv;
	private final String fnCxt;
	private final boolean inTemplate;

	public TypeExprChecker(ErrorReporter errors, RepositoryReader repository, CurrentTCState state, NestedVisitor nv, String fnCxt, boolean inTemplate) {
		this.errors = errors;
		this.repository = repository;
		this.state = state;
		this.nv = nv;
		this.fnCxt = fnCxt;
		this.inTemplate = inTemplate;
		nv.push(this);
	}
	@Override
	public void visitExpr(Expr expr, int nArgs) {
		nv.push(new ExpressionChecker(errors, repository, state, nv, fnCxt, inTemplate));
	}
	
	@Override
	public void leaveTypeExpr(TypeExpr expr) {
		nv.result(LoadBuiltins.type);
		return;
	}

}
