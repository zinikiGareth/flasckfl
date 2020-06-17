package org.flasck.flas.method;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.commonBase.ApplyExpr;
import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.commonBase.MemberExpr;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.AgentDefinition;
import org.flasck.flas.parsedForm.ContractDecl;
import org.flasck.flas.parsedForm.ContractMethodDecl;
import org.flasck.flas.parsedForm.FieldAccessor;
import org.flasck.flas.parsedForm.HandlerImplements;
import org.flasck.flas.parsedForm.MakeAcor;
import org.flasck.flas.parsedForm.MakeSend;
import org.flasck.flas.parsedForm.ObjectActionHandler;
import org.flasck.flas.parsedForm.ObjectContract;
import org.flasck.flas.parsedForm.ObjectDefn;
import org.flasck.flas.parsedForm.ObjectMethod;
import org.flasck.flas.parsedForm.RequiresContract;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.LoadBuiltins;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.repository.RepositoryEntry;
import org.flasck.flas.tc3.NamedType;
import org.flasck.flas.tc3.PolyInstance;
import org.flasck.flas.tc3.Type;
import org.zinutils.exceptions.HaventConsideredThisException;
import org.zinutils.exceptions.NotImplementedException;
import org.zinutils.exceptions.ShouldBeError;

public class MemberExprConvertor extends LeafAdapter {
	private final ErrorReporter errors;
	private final NestedVisitor nv;
	private final ObjectActionHandler oah;
	private Expr obj;
	private ContractDecl cd;
	private ObjectDefn od;
	private StructDefn sd;
	private HandlerImplements hi;
	private FunctionName sendMeth;
	private int expargs;
	private Expr handler;
	private ObjectActionHandler odctor;
	private final Type containerType;
	private final Type containedType;
	private boolean isAcor;
	private FieldAccessor acorFrom;

	public MemberExprConvertor(ErrorReporter errors, NestedVisitor nv, ObjectActionHandler oah, MemberExpr me) {
		this.errors = errors;
		this.nv = nv;
		this.oah = oah;
		containerType = me.containerType();
		containedType = me.containedType();
		nv.push(this);
	}
	
	@Override
	public void visitMemberExpr(MemberExpr expr, int nargs) {
		new MemberExprConvertor(errors, nv, oah, expr);
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
					throw new ShouldBeError("there is no method " + var.var + " on " + cd.name().uniqueName()); // REAL USER ERROR
				sendMeth = cmd.name;
				expargs = cmd.args.size();
			} else if (od != null) {
				FieldAccessor acor = this.od.getAccessor(var.var);
				ObjectActionHandler om = this.od.getMethod(var.var);
				if (om == null) {
					om = this.od.getConstructor(var.var);
					odctor = om;
				}
				if (acor != null) {
					isAcor = true;
					sendMeth = FunctionName.function(var.location(), this.od.name(), var.var);
					expargs = acor.acorArgCount();
				} else if (om == null)
					throw new ShouldBeError("there is no accessor or method " + var.var + " on " + od.name().uniqueName()); // REAL USER ERROR
				else {
					sendMeth = om.name();
					expargs = om.argCount();
				}
			} else if (sd != null) {
				if (containedType == LoadBuiltins.event && var.var.equals("source")) {
					sendMeth = FunctionName.function(var.location(), null, "_eventSource");
					expargs = 0;
				} else {
					FieldAccessor acor = this.sd.getAccessor(var.var);
					if (acor == null)
						throw new NotImplementedException("There is no acor " + var.var);
					acorFrom = acor;
					expargs = 0;
				}
			} else if (hi != null) {
				ObjectMethod hm = this.hi.getMethod(var.var);
				if (hm == null)
					throw new ShouldBeError("there is no accessor or method " + var.var + " on " + od.name().uniqueName()); // REAL USER ERROR
				sendMeth = hm.name();
				expargs = hm.argCount();
			} else {
				throw new NotImplementedException("Need to implement the field case");
			}
		} else
			throw new NotImplementedException("Too many arguments to MemberExpr");
	}

	private void figureDestinationType(RepositoryEntry defn) {
		Type ct = containerType;
		if (ct instanceof PolyInstance) {
			PolyInstance pi = (PolyInstance) ct;
			ct = pi.struct();
		}
		if (ct instanceof ContractDecl)
			this.cd = (ContractDecl) ct;
		else if (ct instanceof ObjectDefn)
			this.od = (ObjectDefn) ct;
		else if (ct instanceof StructDefn)
			this.sd = (StructDefn) ct;
		else if (ct instanceof HandlerImplements)
			this.hi = (HandlerImplements) ct;
		else
			throw new NotImplementedException("cannot handle svc defn of type " + (ct == null ? "NULL" : ct.getClass()) + " for " + defn);
	}

	@Override
	public void leaveMemberExpr(MemberExpr expr) {
		if (odctor != null) {
			convertObjectCtor(expr);
		} else if (acorFrom != null) {
			nv.result(acorFrom.acor(obj));
		} else if (sendMeth != null) {
			if (isAcor)
				nv.result(new MakeAcor(expr.location(), sendMeth, obj, expargs));
			else
				nv.result(new MakeSend(expr.location(), sendMeth, obj, expargs, handler));
		} else
			throw new HaventConsideredThisException("cannot convert " + expr);
	}

	private void convertObjectCtor(MemberExpr expr) {
		UnresolvedVar fn = new UnresolvedVar(expr.fld.location(), ((UnresolvedVar)expr.fld).var);
		fn.bind(odctor);
		List<Expr> args = new ArrayList<>();
		for (ObjectContract oc : od.contracts) {
			if (oah.hasImplements()) {
				NamedType parent = oah.getImplements().getParent();
				if (parent instanceof AgentDefinition) { // This is probably the case for cards as well if we have the right interface
					AgentDefinition ad = (AgentDefinition) parent;
					RequiresContract found = null;
					for (RequiresContract rc : ad.requires) {
						if (rc.actualType() == oc.implementsType().defn()) {
							found = rc;
							break;
						}
					}
					if (found == null) {
						errors.message(expr.fld.location(), "there is no available implementation of " + oc.implementsType().defn().name().uniqueName());
					} else {
						UnresolvedVar ret = new UnresolvedVar(expr.fld.location(), found.referAsVar);
						ret.bind(found);
						args.add(ret);
					}
				} else
					throw new NotImplementedException("there are other valid cases ... and probably invalid ones too");
			} else
				throw new NotImplementedException("there are other valid cases ... and probably invalid ones too");
		}
		if (args.isEmpty())
			nv.result(fn);
		else
			nv.result(new ApplyExpr(expr.location, fn, (Object[])args.toArray(new Expr[args.size()])));
	}
}
