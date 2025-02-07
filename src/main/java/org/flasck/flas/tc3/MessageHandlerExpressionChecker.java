package org.flasck.flas.tc3;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.commonBase.MemberExpr;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.repository.RepositoryReader;
import org.flasck.flas.repository.ResultAware;
import org.flasck.flas.tc3.ExpressionChecker.ExprResult;

public class MessageHandlerExpressionChecker extends LeafAdapter implements ResultAware {
	private final ErrorReporter errors;
	private final RepositoryReader repository;
	private final NestedVisitor nv;
	private final List<PosType> results = new ArrayList<>();
	private final CurrentTCState state;
	private final String fnCxt;

	public MessageHandlerExpressionChecker(ErrorReporter errors, RepositoryReader repository, CurrentTCState state, NestedVisitor nv, String fnCxt) {
		this.errors = errors;
		this.repository = repository;
		this.state = state;
		this.nv = nv;
		this.fnCxt = fnCxt;
	}
	
	@Override
	public void visitExpr(Expr expr, int nArgs) {
		nv.push(new ExpressionChecker(errors, repository, state, nv, fnCxt, false));
	}
	
	@Override
	public boolean visitMemberExpr(MemberExpr expr, int nargs) {
		nv.push(new MemberExpressionChecker(errors, repository, state, nv, fnCxt, false));
		return false;
	}
	
	@Override
	public void result(Object r) {
		ExprResult ty = (ExprResult) r;
		if (ty == null || ty.type == null) {
			throw new NullPointerException("Cannot handle null type");
		}
		results.add(ty);
	}
}
