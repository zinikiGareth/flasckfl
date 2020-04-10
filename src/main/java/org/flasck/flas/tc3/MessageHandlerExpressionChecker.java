package org.flasck.flas.tc3;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.commonBase.MemberExpr;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.repository.ResultAware;
import org.flasck.flas.tc3.ExpressionChecker.ExprResult;

public class MessageHandlerExpressionChecker extends LeafAdapter implements ResultAware {
	private final ErrorReporter errors;
	private final NestedVisitor nv;
	private final List<PosType> results = new ArrayList<>();
	private final CurrentTCState state;

	public MessageHandlerExpressionChecker(ErrorReporter errors, CurrentTCState state, NestedVisitor nv) {
		this.errors = errors;
		this.state = state;
		this.nv = nv;
	}
	
	@Override
	public void visitExpr(Expr expr, int nArgs) {
		nv.push(new ExpressionChecker(errors, state, nv));
	}
	
	@Override
	public void visitMemberExpr(MemberExpr expr) {
		nv.push(new MemberExpressionChecker(errors, state, nv));
	}
	
	@Override
	public void result(Object r) {
		ExprResult ty = (ExprResult) r;
		if (ty == null || ty.type == null) {
			throw new NullPointerException("Cannot handle null type");
		}
		results.add(ty);
	}
	
	@Override
	public void leaveHandleExpr(Expr expr, Expr handler) {
		PosType pfn = results.remove(0);
		Type fn = pfn.type;
		if (fn instanceof ErrorType) {
			nv.result(fn);
			return;
		} else if (fn instanceof UnifiableType) {
			UnifiableType ut = (UnifiableType)fn;
			nv.result(ut.canBeAppliedTo(expr.location(), results));
			return;
		} else if (fn.argCount() < results.size()) {
			errors.message(pfn.pos, fn + " expects: " + fn.argCount() + " has: " + results.size());
			nv.result(new ErrorType());
			return;
		}
		PosType pai = results.remove(0);
		Type ai = pai.type;
		if (ai instanceof ErrorType) {
			nv.result(pai);
			return;
		}
		if (fn.argCount() != 1) {
			errors.message(pfn.pos, "-> requires exactly one handler");
			nv.result(new ErrorType());
			return;
		}
		Type fi = fn.get(0);
		InputPosition loc = handler.location();
		if (!fi.incorporates(loc, ai)) {
			errors.message(loc, "handler should be " + fi.signature() + " not " + ai.signature());
			nv.result(new ErrorType());
			return;
		}
		nv.result(fn.get(1));
	}
}
