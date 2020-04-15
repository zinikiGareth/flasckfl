package org.flasck.flas.tc3;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.commonBase.MemberExpr;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.ContractDecl;
import org.flasck.flas.parsedForm.ContractMethodDecl;
import org.flasck.flas.parsedForm.FieldAccessor;
import org.flasck.flas.parsedForm.ObjectCtor;
import org.flasck.flas.parsedForm.ObjectDefn;
import org.flasck.flas.parsedForm.ObjectMethod;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.StructField;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.repository.ResultAware;
import org.flasck.flas.tc3.ExpressionChecker.ExprResult;
import org.zinutils.exceptions.NotImplementedException;

public class MemberExpressionChecker extends LeafAdapter implements ResultAware {
	private final ErrorReporter errors;
	private final NestedVisitor nv;
	private final List<Type> results = new ArrayList<>();
	private final CurrentTCState state;

	public MemberExpressionChecker(ErrorReporter errors, CurrentTCState state, NestedVisitor nv) {
		this.errors = errors;
		this.state = state;
		this.nv = nv;
	}
	
	@Override
	public void visitExpr(Expr expr, int nArgs) {
		nv.push(new ExpressionChecker(errors, state, nv));
	}
	
	@Override
	public void result(Object r) {
		if (r == null) {
			throw new NullPointerException("Cannot handle null type");
		}
		results.add(((ExprResult) r).type);
	}

	@Override
	public void leaveMemberExpr(MemberExpr expr) {
		Type ty = results.get(0);
		if (!(expr.fld instanceof UnresolvedVar))
			throw new NotImplementedException("Cannot handle " + expr.fld);
		UnresolvedVar fld = (UnresolvedVar)expr.fld;
		if (ty instanceof ContractDecl) {
			ContractDecl cd = (ContractDecl) ty;
			ContractMethodDecl method = cd.getMethod(fld.var);
			if (method == null) {
				errors.message(fld.location(), "there is no method '" + fld.var + "' in " + cd.name().uniqueName());
				nv.result(new ErrorType());
			} else {
				nv.result(method.type());
				expr.bindContractMethod(method);
			}
		} else if (ty instanceof StructDefn) {
			StructDefn sd = (StructDefn) ty;
			StructField sf = sd.findField(fld.var);
			if (sf == null) {
				throw new NotImplementedException();
			} else {
				nv.result(sf.type.defn());
			}
		} else if (ty instanceof ObjectDefn) {
			ObjectDefn od = (ObjectDefn) ty;
			FieldAccessor fa = od.getAccessor(fld.var);
			if (fa != null) {
				nv.result(fa.type());
				return;
			}
			ObjectCtor ctor = od.getConstructor(fld.var);
			if (ctor != null) {
				nv.result(ctor.type());
				return;
			}
			ObjectMethod meth = od.getMethod(fld.var);
			if (meth != null) {
				nv.result(meth.type());
				return;
			}
			throw new NotImplementedException("object " + od.name() + " does not have acor or ctor");
		} else if (expr.from instanceof UnresolvedVar) {
			UnresolvedVar var = (UnresolvedVar) expr.from;
			errors.message(expr.from.location(), "there is insufficient information to deduce the type of '" + var.var + "' in order to apply it to '" + fld.var + "'");
			nv.result(new ErrorType());
		} else
			throw new NotImplementedException("Not yet handled: " + ty);
	}
}
