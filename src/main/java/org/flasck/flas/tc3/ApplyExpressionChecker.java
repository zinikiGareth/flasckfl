package org.flasck.flas.tc3;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.commonBase.ApplyExpr;
import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.repository.RepositoryReader;
import org.flasck.flas.repository.ResultAware;

public class ApplyExpressionChecker extends LeafAdapter implements ResultAware {
	private final RepositoryReader r;
	private final NestedVisitor nv;
	private final List<Type> results = new ArrayList<>();
	private final CurrentTCState state;

	public ApplyExpressionChecker(RepositoryReader repository, CurrentTCState state, NestedVisitor nv) {
		this.r = repository;
		this.state = state;
		this.nv = nv;
	}
	
	@Override
	public void visitExpr(Expr expr, int nArgs) {
		nv.push(new ExpressionChecker(r, state, nv));
	}
	
	@Override
	public void result(Object r) {
		if (r == null)
			throw new NullPointerException("Cannot handle null type");
		results.add((Type) r);
	}

	@Override
	public void leaveApplyExpr(ApplyExpr expr) {
		Type fn = results.remove(0);
		if (fn.argCount() != results.size())
			throw new RuntimeException("should be an error or a curry case");
		int pos = 0;
		while (!results.isEmpty()) {
			/* ai = */ results.remove(0);
			// TODO: should check type of ai
			pos++;
		}
		// whatever is left is the type
		if (results.size() > 1)
			throw new RuntimeException("need to build up a function type");
		nv.result(fn.get(pos));
	}
}
