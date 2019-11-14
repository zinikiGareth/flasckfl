package org.flasck.flas.method;

import org.flasck.flas.commonBase.MemberExpr;
import org.flasck.flas.repository.LeafAdapter;
import org.zinutils.exceptions.NotImplementedException;

public class AccessorConvertor extends LeafAdapter {
	public AccessorConvertor() {
	}

	@Override
	public void visitMemberExpr(MemberExpr expr) {
		throw new NotImplementedException();
	}
}
