package org.flasck.flas.method;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.commonBase.ApplyExpr;
import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.commonBase.MemberExpr;
import org.flasck.flas.parsedForm.ActionMessage;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.repository.ResultAware;
import org.zinutils.exceptions.NotImplementedException;

public class MessageConvertor extends LeafAdapter implements ResultAware {
	private final NestedVisitor nv;
	private final List<Object> stack = new ArrayList<>();

	public MessageConvertor(NestedVisitor nv) {
		this.nv = nv;
	}

	@Override
	public void visitExpr(Expr expr, int nArgs) {
		if (expr instanceof ApplyExpr)
			nv.push(new MessageConvertor(nv));
		else if (expr instanceof MemberExpr)
			nv.push(new MemberExprConvertor(nv));
		else
			stack.add(expr);
	}

	@Override
	public void result(Object r) {
		stack.add((Expr) r);
	}
	
	@Override
	public void leaveApplyExpr(ApplyExpr expr) {
		Object op = stack.remove(0);
		nv.result(new ApplyExpr(expr.location(), op, stack));
	}

	@Override
	public void leaveMessage(ActionMessage msg) {
		if (stack.size() != 1)
			throw new NotImplementedException("should be 1");
		nv.result(stack.remove(0));
	}
}
