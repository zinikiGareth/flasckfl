package org.flasck.flas.method;

import org.flasck.flas.commonBase.MemberExpr;
import org.flasck.flas.parsedForm.MakeSend;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.NestedVisitor;

public class MemberExprConvertor extends LeafAdapter {
	private NestedVisitor nv;

	public MemberExprConvertor(NestedVisitor nv) {
		this.nv = nv;
	}
	
	// TODO: this needs to collect the two parameters
	@Override
	public void visitUnresolvedVar(UnresolvedVar var, int nargs) {
		super.visitUnresolvedVar(var, nargs);
	}

	@Override
	public void leaveMemberExpr(MemberExpr expr) {
		nv.result(new MakeSend(expr.location(), 0));
	}
}
