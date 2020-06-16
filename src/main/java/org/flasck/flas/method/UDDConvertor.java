package org.flasck.flas.method;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.commonBase.ApplyExpr;
import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.commonBase.MemberExpr;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parser.ut.UnitDataDeclaration;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.repository.ResultAware;
import org.zinutils.exceptions.NotImplementedException;

public class UDDConvertor extends LeafAdapter implements ResultAware {
	private final NestedVisitor nv;
	private final ErrorReporter errors;
	private final List<Object> stack = new ArrayList<>();
	private MemberExpr convert;

	public UDDConvertor(NestedVisitor sv, ErrorReporter errors) {
		this.nv = sv;
		this.errors = errors;
		sv.push(this);
	}

	@Override
	public void visitExpr(Expr expr, int nArgs) {
		if (expr instanceof ApplyExpr)
			new UDDConvertor(nv, errors);
		else if (expr instanceof MemberExpr) {
			convert = (MemberExpr) expr;
		} else
			stack.add(expr);
	}
	
	@Override
	public void visitMemberExpr(MemberExpr expr, int nargs) {
		new MemberExprConvertor(errors, nv, null, convert);
	}
	
	@Override
	public void result(Object r) {
		if (convert != null) {
			convert.conversion((Expr) r);
			convert = null;
		}
		stack.add(r);
	}
	
	@Override
	public void leaveApplyExpr(ApplyExpr expr) {
		Object op = stack.remove(0);
		nv.result(new ApplyExpr(expr.location(), op, stack));
	}
	
	@Override
	public void leaveUnitDataDeclaration(UnitDataDeclaration udd) {
		if (stack.size() == 1)
			stack.remove(0);
		if (!stack.isEmpty())
			throw new NotImplementedException("the stack should be empty");
		nv.result(null);
	}
}
