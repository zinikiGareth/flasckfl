package org.flasck.flas.method;

import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.commonBase.MemberExpr;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.TemplateBindingOption;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.repository.ResultAware;

public class TemplateConvertor extends LeafAdapter implements ResultAware {
	private final ErrorReporter errors;
	private final NestedVisitor nv;
	private MemberExpr remember;

	public TemplateConvertor(ErrorReporter errors, NestedVisitor sv) {
		this.errors = errors;
		this.nv = sv;
		sv.push(this);
	}

	@Override
	public void visitMemberExpr(MemberExpr expr) {
		remember = expr;
		nv.push(new MemberExprConvertor(errors, nv, null));
	}

	@Override
	public void result(Object r) {
		remember.conversion((Expr) r);
	}
	
	@Override
	public void leaveTemplateBindingOption(TemplateBindingOption option) {
		nv.result(null);
	}
}
