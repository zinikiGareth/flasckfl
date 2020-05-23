package org.flasck.flas.method;

import org.flasck.flas.commonBase.MemberExpr;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.repository.LeafAdapter;

public class CheckNoMessages extends LeafAdapter {
	private final ErrorReporter errors;

	public CheckNoMessages(ErrorReporter errors) {
		this.errors = errors;
	}

	@Override
	public void visitMemberExpr(MemberExpr expr) {
		errors.message(expr.location(), "cannot call a method initializer in a field initializer");
	}
}
