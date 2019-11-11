package org.flasck.flas.method;

import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.commonBase.MemberExpr;
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
	
	// TODO: this needs to collect the two parameters
	@Override
	public void visitUnresolvedVar(UnresolvedVar var, int nargs) {
		if (obj == null) {
			obj = var;
			this.cd = resolveContract(var.defn());
		} else if (sendMeth == null) {
			cmd = this.cd.getMethod(var.var);
			if (cmd == null)
				throw new NotImplementedException("there is no method " + var.var + " on " + cd.name().uniqueName()); // REAL USER ERROR
			sendMeth = cmd.name;
		}
	}

	private ContractDecl resolveContract(RepositoryEntry defn) {
		if (defn instanceof TypedPattern) {
			TypedPattern tp = (TypedPattern) defn;
			return (ContractDecl) tp.type.defn();
		} else
			throw new NotImplementedException("cannot handle svc of type " + (defn == null ? "NULL" : defn.getClass()));
	}

	@Override
	public void leaveMemberExpr(MemberExpr expr) {
		nv.result(new MakeSend(expr.location(), sendMeth, obj, cmd.args.size()));
	}
}
