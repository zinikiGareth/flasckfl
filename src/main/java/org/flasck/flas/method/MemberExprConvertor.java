package org.flasck.flas.method;

import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.commonBase.MemberExpr;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.parsedForm.ContractDecl;
import org.flasck.flas.parsedForm.ContractMethodDecl;
import org.flasck.flas.parsedForm.FieldAccessor;
import org.flasck.flas.parsedForm.HandlerLambda;
import org.flasck.flas.parsedForm.IntroduceVar;
import org.flasck.flas.parsedForm.MakeSend;
import org.flasck.flas.parsedForm.ObjectDefn;
import org.flasck.flas.parsedForm.ObjectMethod;
import org.flasck.flas.parsedForm.RequiresContract;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.TypedPattern;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.parsedForm.VarPattern;
import org.flasck.flas.parser.ut.UnitDataDeclaration;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.repository.RepositoryEntry;
import org.flasck.flas.tc3.NamedType;
import org.zinutils.exceptions.NotImplementedException;

public class MemberExprConvertor extends LeafAdapter {
	private NestedVisitor nv;
	private Expr obj;
	private ContractDecl cd;
	private ObjectDefn od;
	private StructDefn sd;
	private FunctionName sendMeth;
	private int expargs;
	private Expr handler;

	public MemberExprConvertor(NestedVisitor nv) {
		this.nv = nv;
	}
	
	@Override
	public void visitUnresolvedVar(UnresolvedVar var, int nargs) {
		if (obj == null) {
			obj = var;
			figureDestinationType(var.defn());
		} else if (sendMeth == null) {
			if (cd != null) {
				ContractMethodDecl cmd = this.cd.getMethod(var.var);
				if (cmd == null)
					throw new NotImplementedException("there is no method " + var.var + " on " + cd.name().uniqueName()); // REAL USER ERROR
				sendMeth = cmd.name;
				expargs = cmd.args.size();
			} else if (od != null) {
				FieldAccessor acor = this.od.getAccessor(var.var);
				if (acor != null)
					throw new NotImplementedException("I don't think I've handled that case yet but it is valid");
				ObjectMethod om = this.od.getMethod(var.var);
				if (om == null)
					throw new NotImplementedException("there is no accessor or method " + var.var + " on " + od.name().uniqueName()); // REAL USER ERROR
				sendMeth = om.name();
				expargs = om.argCount();
			} else if (sd != null) {
				FieldAccessor acor = this.sd.getAccessor(var.var);
				if (acor == null)
					throw new NotImplementedException("There is no acor " + var.var);
				sendMeth = FunctionName.function(var.location(), this.sd.name(), var.var);
				expargs = 0;
			} else {
				throw new NotImplementedException("Need to implement the field case");
			}
		} else
			throw new NotImplementedException("Too many arguments to MemberExpr");
	}

	private void figureDestinationType(RepositoryEntry defn) {
		NamedType dt;
		if (defn instanceof TypedPattern) {
			TypedPattern tp = (TypedPattern) defn;
			dt = tp.type.defn();
		} else if (defn instanceof HandlerLambda) {
			TypedPattern tp = (TypedPattern) ((HandlerLambda)defn).patt;
			dt = tp.type.defn();
		} else if (defn instanceof VarPattern) {
			VarPattern vp = (VarPattern) defn;
			if (vp.type() == null) {
				throw new NotImplementedException("cannot use var " + vp + " as it is not bound to a type");
			} else
				dt = (NamedType) vp.type();
		} else if (defn instanceof RequiresContract) {
			dt = ((RequiresContract)defn).actualType();
		} else if (defn instanceof UnitDataDeclaration) {
			dt = ((UnitDataDeclaration)defn).ofType.defn();
		} else if (defn instanceof IntroduceVar) {
			dt = (NamedType) ((IntroduceVar)defn).introducedAs();
		} else
			throw new NotImplementedException("cannot handle svc var of type " + (defn == null ? "NULL" : defn.getClass()));
		if (dt instanceof ContractDecl)
			this.cd = (ContractDecl) dt;
		else if (dt instanceof ObjectDefn)
			this.od = (ObjectDefn) dt;
		else if (dt instanceof StructDefn)
			this.sd = (StructDefn) dt;
		else
			throw new NotImplementedException("cannot handle svc defn of type " + (dt == null ? "NULL" : dt.getClass()));
	}

	@Override
	public void leaveMemberExpr(MemberExpr expr) {
		if (sendMeth != null)
			nv.result(new MakeSend(expr.location(), sendMeth, obj, expargs, handler));
		else
			throw new NotImplementedException("Need to implement the field case");
	}
}
