package org.flasck.flas.method;

import org.flasck.flas.commonBase.ApplyExpr;
import org.flasck.flas.commonBase.MemberExpr;
import org.flasck.flas.commonBase.NumericLiteral;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.parsedForm.UnresolvedOperator;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.repository.ResultAware;

public class MessageConvertor extends LeafAdapter implements ResultAware {
	private final NestedVisitor nv;

	public MessageConvertor(NestedVisitor nv) {
		this.nv = nv;
	}

	@Override
	public void visitMemberExpr(MemberExpr expr) {
		nv.push(new MemberExprConvertor(nv));
	}

	@Override
	public void visitStringLiteral(StringLiteral expr) {
		nv.result(expr);
	}
	
	@Override
	public void visitNumericLiteral(NumericLiteral expr) {
		nv.result(expr);
	}
	
	@Override
	public void visitUnresolvedVar(UnresolvedVar var, int nargs) {
		nv.result(var);
	}

	@Override
	public void visitUnresolvedOperator(UnresolvedOperator var, int nargs) {
		nv.result(var);
	}

	@Override
	public void visitApplyExpr(ApplyExpr expr) {
		nv.push(new MessageConvertor(nv));
	}
	
	@Override
	public void result(Object r) {
		nv.result(r);
	}
}
