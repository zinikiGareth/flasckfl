package org.flasck.flas.method;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.commonBase.ApplyExpr;
import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.commonBase.MemberExpr;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.ActionMessage;
import org.flasck.flas.parsedForm.AssignMessage;
import org.flasck.flas.parsedForm.CurrentContainer;
import org.flasck.flas.parsedForm.MakeSend;
import org.flasck.flas.parsedForm.ObjectActionHandler;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.parsedForm.ut.UnitTestInvoke;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.LoadBuiltins;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.repository.ResultAware;
import org.zinutils.exceptions.NotImplementedException;

public class MessageConvertor extends LeafAdapter implements ResultAware {
	private final ErrorReporter errors;
	private final NestedVisitor nv;
	private final ObjectActionHandler oah;
	private final List<Object> stack = new ArrayList<>();

	public MessageConvertor(ErrorReporter errors, NestedVisitor nv, ObjectActionHandler oah) {
		this.errors = errors;
		this.nv = nv;
		this.oah = oah;
	}

	@Override
	public void visitExpr(Expr expr, int nArgs) {
		if (expr instanceof ApplyExpr)
			nv.push(new MessageConvertor(errors, nv, oah));
		else if (expr instanceof MemberExpr)
			nv.push(new MemberExprConvertor(errors, nv, oah));
		else
			stack.add(expr);
	}

	@Override
	public void result(Object r) {
		stack.add((Expr) r);
	}
	
	@Override
	public void leaveAssignMessage(AssignMessage msg) {
		if (stack.size() != 1)
			throw new NotImplementedException("should be 1");
		Expr expr = (Expr) stack.remove(0);
		UnresolvedVar op = new UnresolvedVar(msg.kw, "Assign");
		op.bind(LoadBuiltins.assign);
		UnresolvedVar first = msg.slot.get(0);
		stack.add(new ApplyExpr(msg.kw, op, new CurrentContainer(msg.kw), new StringLiteral(first.location, first.var), expr));
	}
	
	@Override
	public void leaveApplyExpr(ApplyExpr expr) {
		Object op = stack.remove(0);
		nv.result(new ApplyExpr(expr.location(), op, stack));
	}

	@Override
	public void leaveHandleExpr(Expr expr, Expr handler) {
		MakeSend ms = (MakeSend) stack.remove(0);
		Expr h = (Expr) stack.remove(0);
		ms.handler = h;
		nv.result(ms);
	}
	
	@Override
	public void leaveMessage(ActionMessage msg) {
		if (stack.size() != 1)
			throw new NotImplementedException("when sending messages, should only have 1 arg");
		nv.result(stack.remove(0));
	}
	
	@Override
	public void leaveUnitTestInvoke(UnitTestInvoke uti) {
		if (stack.size() != 1)
			throw new NotImplementedException("should be 1");
		uti.conversion((Expr) stack.remove(0));
		nv.result(null);
	}
}
