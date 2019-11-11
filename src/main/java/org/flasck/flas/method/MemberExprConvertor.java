package org.flasck.flas.method;

import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.commonBase.MemberExpr;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.parsedForm.ContractDecl;
import org.flasck.flas.parsedForm.ContractMethodDecl;
import org.flasck.flas.parsedForm.MakeSend;
import org.flasck.flas.parsedForm.TypedPattern;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.repository.RepositoryEntry;
import org.zinutils.exceptions.NotImplementedException;

public class MemberExprConvertor extends LeafAdapter {
	private NestedVisitor nv;
	private Expr obj;
	private ContractDecl cd;
	private FunctionName sendMeth;
	private ContractMethodDecl cmd;

	public MemberExprConvertor(NestedVisitor nv) {
		this.nv = nv;
	}
	
	@Override
	public void visitUnresolvedVar(UnresolvedVar var, int nargs) {
		if (obj == null) {
			obj = var;
			// TODO: this just "assumes" the method case; we also need to consider the field case
			this.cd = resolveContract(var.defn());
		} else if (sendMeth == null) {
			if (cd != null) {
				cmd = this.cd.getMethod(var.var);
				if (cmd == null)
					throw new NotImplementedException("there is no method " + var.var + " on " + cd.name().uniqueName()); // REAL USER ERROR
				sendMeth = cmd.name;
			} else {
				sendMeth = FunctionName.contractMethod(var.location(), null, "handle_the_field_case");
			}
		}
	}

	private ContractDecl resolveContract(RepositoryEntry defn) {
		if (defn instanceof TypedPattern) {
			TypedPattern tp = (TypedPattern) defn;
			RepositoryEntry dt = tp.type.defn();
			if (dt instanceof ContractDecl)
				return (ContractDecl) tp.type.defn();
			else
				return null;
		} else
			throw new NotImplementedException("cannot handle svc of type " + (defn == null ? "NULL" : defn.getClass()));
	}

	@Override
	public void leaveMemberExpr(MemberExpr expr) {
		if (cmd != null)
			nv.result(new MakeSend(expr.location(), sendMeth, obj, cmd.args.size()));
		else
			nv.result(new StringLiteral(expr.location(), "need_to_implement_field_case"));
	}
}
