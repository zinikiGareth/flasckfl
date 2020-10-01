package org.flasck.flas.tc3;

import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.CastExpr;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.repository.RepositoryReader;
import org.flasck.flas.repository.ResultAware;
import org.flasck.flas.tc3.ExpressionChecker.ExprResult;
import org.zinutils.exceptions.CantHappenException;

public class CastExprChecker extends LeafAdapter implements ResultAware {
	private final ErrorReporter errors;
	private final RepositoryReader repository;
	private final CurrentTCState state;
	private final NestedVisitor nv;
	private final String fnCxt;
	private final boolean inTemplate;
	private NamedType castTo;
	private ExprResult original;

	public CastExprChecker(ErrorReporter errors, RepositoryReader repository, CurrentTCState state, NestedVisitor nv, String fnCxt, boolean inTemplate) {
		this.errors = errors;
		this.repository = repository;
		this.state = state;
		this.nv = nv;
		this.fnCxt = fnCxt;
		this.inTemplate = inTemplate;
		nv.push(this);
	}
	
	@Override
	public void visitTypeReference(TypeReference var, boolean expectPolys, int exprNargs) {
		castTo = var.defn();
	}
	
	@Override
	public void visitExpr(Expr expr, int nArgs) {
		nv.push(new ExpressionChecker(errors, repository, state, nv, fnCxt, inTemplate));
	}
	
	@Override
	public void result(Object r) {
		if (this.original == null)
			this.original = (ExprResult) r;
		else
			throw new CantHappenException("too many results");
	}

	@Override
	public void leaveCastExpr(CastExpr expr) {
		nv.result(castTo);
		return;
	}

}
