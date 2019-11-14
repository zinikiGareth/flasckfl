package org.flasck.flas.method;

import org.flasck.flas.commonBase.MemberExpr;
import org.flasck.flas.parsedForm.MakeAcor;
import org.flasck.flas.repository.LeafAdapter;

public class AccessorConvertor extends LeafAdapter {
	public AccessorConvertor() {
	}

	@Override
	public void visitMemberExpr(MemberExpr expr) {
		expr.conversion(new MakeAcor(null, null, expr.from, 0));
	}
}
