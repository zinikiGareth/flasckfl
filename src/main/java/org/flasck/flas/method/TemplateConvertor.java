package org.flasck.flas.method;

import org.flasck.flas.commonBase.MemberExpr;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.TemplateBindingOption;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.NestedVisitor;

public class TemplateConvertor extends LeafAdapter {
	private final ErrorReporter errors;
	private final NestedVisitor nv;

	public TemplateConvertor(ErrorReporter errors, NestedVisitor sv) {
		this.errors = errors;
		this.nv = sv;
		sv.push(this);
	}

	@Override
	public void visitMemberExpr(MemberExpr expr) {
		AccessorConvertor acor = new AccessorConvertor(nv, errors, null);
		acor.visitMemberExpr(expr);
	}

	@Override
	public void leaveTemplateBindingOption(TemplateBindingOption option) {
		nv.result(null);
	}
}
