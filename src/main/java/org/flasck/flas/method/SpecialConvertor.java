package org.flasck.flas.method;

import org.flasck.flas.commonBase.ApplyExpr;
import org.flasck.flas.commonBase.MemberExpr;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.AssignMessage;
import org.flasck.flas.parsedForm.CastExpr;
import org.flasck.flas.parsedForm.ObjectActionHandler;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.repository.ResultAware;

public class SpecialConvertor extends LeafAdapter implements ResultAware {

	private final ErrorReporter errors;
	private final NestedVisitor nv;
	private final ObjectActionHandler oah;
	private Object res;

	public SpecialConvertor(ErrorReporter errors, NestedVisitor nv, ObjectActionHandler oah, AssignMessage assign) {
		this.errors = errors;
		this.nv = nv;
		this.oah = oah;
	}
	
	@Override
	public void visitApplyExpr(ApplyExpr expr) {
		nv.push(new MessageConvertor(errors, nv, oah, null));
	}
	
	@Override
	public boolean visitMemberExpr(MemberExpr expr, int nargs) {
		new MemberExprConvertor(errors, nv, oah, (MemberExpr) expr);
		return false;
	}

	@Override
	public void result(Object r) {
		this.res = r;
	}

	@Override
	public void leaveCastExpr(CastExpr expr) {
		nv.result(res);
	}
}
