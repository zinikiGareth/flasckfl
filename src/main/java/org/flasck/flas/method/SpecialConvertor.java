package org.flasck.flas.method;

import org.flasck.flas.commonBase.ApplyExpr;
import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.commonBase.MemberExpr;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.AssignMessage;
import org.flasck.flas.parsedForm.CastExpr;
import org.flasck.flas.parsedForm.ObjectActionHandler;
import org.flasck.flas.parsedForm.TypeExpr;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.repository.ResultAware;
import org.flasck.flas.tc3.NamedType;

public class SpecialConvertor extends LeafAdapter implements ResultAware {
	private final ErrorReporter errors;
	private final NestedVisitor nv;
	private final ObjectActionHandler oah;
	private TypeReference ty;
	private Expr value;

	public SpecialConvertor(ErrorReporter errors, NestedVisitor nv, ObjectActionHandler oah, AssignMessage assign) {
		this.errors = errors;
		this.nv = nv;
		this.oah = oah;
		nv.push(this);
	}

	@Override
	public void visitApplyExpr(ApplyExpr expr) {
		nv.push(new MessageConvertor(errors, nv, oah, null));
	}
	
	@Override
	public boolean visitMemberExpr(MemberExpr expr, int nargs) {
		if (expr.boundEarly()) {
			NamedType nt = (NamedType)expr.defn();
			TypeReference tr = new TypeReference(expr.location, nt.name().baseName());
			tr.bind(nt);
			this.ty = tr;
			return true;
		}
		new MemberExprConvertor(errors, nv, oah, (MemberExpr) expr);
		return false;
	}

	@Override
	public void result(Object r) {
		if (ty == null)
			ty = (TypeReference) r;
		else if (value == null)
			value = (Expr) r;
	}

	@Override
	public void leaveTypeExpr(TypeExpr expr) {
		if (ty == null)
			nv.result(expr);
		else
			nv.result(new TypeExpr(expr.location, expr.tyLoc, ty));
	}

	@Override
	public void leaveCastExpr(CastExpr expr) {
		if (ty == null)
			nv.result(expr);
		else
			nv.result(new CastExpr(expr.location, expr.tyLoc, expr.valLoc, ty, value));
	}
}
