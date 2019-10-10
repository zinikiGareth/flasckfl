package org.flasck.flas.tc3;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.ApplyExpr;
import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.commonBase.Locatable;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.UnresolvedOperator;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.repository.RepositoryReader;
import org.flasck.flas.repository.ResultAware;

public class ApplyExpressionChecker extends LeafAdapter implements ResultAware {
	private final ErrorReporter errors;
	private final RepositoryReader repository;
	private final NestedVisitor nv;
	private final List<Type> results = new ArrayList<>();
	private final CurrentTCState state;

	public ApplyExpressionChecker(ErrorReporter errors, RepositoryReader repository, CurrentTCState state, NestedVisitor nv) {
		this.errors = errors;
		this.repository = repository;
		this.state = state;
		this.nv = nv;
	}
	
	@Override
	public void visitExpr(Expr expr, int nArgs) {
		nv.push(new ExpressionChecker(errors, repository, state, nv));
	}
	
	@Override
	public void result(Object r) {
		if (r == null) {
			throw new NullPointerException("Cannot handle null type");
		}
		results.add((Type) r);
	}

	@Override
	public void leaveApplyExpr(ApplyExpr expr) {
		if (expr.fn instanceof UnresolvedOperator && ((UnresolvedOperator)expr.fn).op.equals("[]")) {
			handleListBuilder(expr);
			return;
		}
		Type fn = results.remove(0);
		if (fn.argCount() != results.size())
			throw new RuntimeException("should be an error or a curry case: " + fn + " " + fn.argCount() + " " + results.size());
		int pos = 0;
		while (!results.isEmpty()) {
			Type ai = results.remove(0);
			if (ai instanceof ErrorType) {
				nv.result(ai);
				return;
			}
			InputPosition loc = ((Locatable)expr.args.get(pos)).location();
			Type fi = fn.get(pos);
			if (ai instanceof UnifiableType) {
				UnifiableType ut = (UnifiableType) ai;
				ut.incorporatedBy(loc, fi);
			} else if (!fi.incorporates(ai)) {
				errors.message(loc, "typing: " + fi + " " + ai);
				nv.result(new ErrorType());
				return;
			}
			pos++;
		}
		// whatever is left is the type
		if (results.size() > 1)
			throw new RuntimeException("need to build up a function type");
		nv.result(fn.get(pos));
	}

	private void handleListBuilder(ApplyExpr expr) {
		if (expr.args.isEmpty()) {
			nv.result(repository.get("Nil"));
		} else {
			// TODO: I think we need to check all the arguments TCed OK
			// I think we need to make sure that they are all of the same type, but can we allow a "mixed" list?
			// Specifically, surely we can allow a list of Union types?  Even if only some of them are included?
			nv.result(repository.get("Cons"));
		}
	}
}
