package org.flasck.flas.tc3;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.commonBase.MemberExpr;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.ContractDecl;
import org.flasck.flas.parsedForm.ContractMethodDecl;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.repository.ResultAware;
import org.flasck.flas.tc3.ExpressionChecker.ExprResult;
import org.zinutils.exceptions.NotImplementedException;

public class MemberExpressionChecker extends LeafAdapter implements ResultAware {
	private final ErrorReporter errors;
	private final NestedVisitor nv;
	private final List<Type> results = new ArrayList<>();
	private final CurrentTCState state;

	public MemberExpressionChecker(ErrorReporter errors, CurrentTCState state, NestedVisitor nv) {
		this.errors = errors;
		this.state = state;
		this.nv = nv;
	}
	
	@Override
	public void visitExpr(Expr expr, int nArgs) {
		nv.push(new ExpressionChecker(errors, state, nv));
	}
	
	@Override
	public void result(Object r) {
		if (r == null) {
			throw new NullPointerException("Cannot handle null type");
		}
		results.add(((ExprResult) r).type);
	}

	@Override
	public void leaveMemberExpr(MemberExpr expr) {
		Type ty = results.get(0);
		if (ty instanceof ContractDecl) {
			ContractDecl cd = (ContractDecl) ty;
			if (!(expr.fld instanceof UnresolvedVar))
				throw new NotImplementedException("Cannot handle " + expr.fld);
			ContractMethodDecl method = cd.getMethod(((UnresolvedVar)expr.fld).var);
			nv.result(method.type());
		} else
			throw new NotImplementedException("Not yet handled: " + ty);
	}
}
