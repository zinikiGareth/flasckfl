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
import org.flasck.flas.parsedForm.HandlerLambda;
import org.flasck.flas.parsedForm.IntroduceVar;
import org.flasck.flas.parsedForm.MakeSend;
import org.flasck.flas.parsedForm.ObjectActionHandler;
import org.flasck.flas.parsedForm.ObjectContract;
import org.flasck.flas.parsedForm.ObjectDefn;
import org.flasck.flas.parsedForm.ObjectMethod;
import org.flasck.flas.parsedForm.RequiresContract;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.StructField;
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

	public MemberExprConvertor(ErrorReporter errors, NestedVisitor nv, ObjectActionHandler oah) {
		this.errors = errors;
		this.nv = nv;
		this.oah = oah;
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
				ObjectActionHandler om = this.od.getMethod(var.var);
				if (om == null) {
					om = this.od.getConstructor(var.var);
					odctor = om;
				}
				if (acor != null) {
					sendMeth = FunctionName.function(var.location(), this.od.name(), var.var);
					expargs = acor.type().argCount();
				} else if (om == null)
					throw new NotImplementedException("there is no accessor or method " + var.var + " on " + od.name().uniqueName()); // REAL USER ERROR
				else {
					sendMeth = om.name();
					expargs = om.argCount();
				}
			} else if (sd != null) {
				FieldAccessor acor = this.sd.getAccessor(var.var);
				if (acor == null)
					throw new NotImplementedException("There is no acor " + var.var);
				sendMeth = FunctionName.function(var.location(), this.sd.name(), var.var);
				expargs = 0;
			} else if (hi != null) {
				ObjectMethod hm = this.hi.getMethod(var.var);
				if (hm == null)
					throw new NotImplementedException("there is no accessor or method " + var.var + " on " + od.name().uniqueName()); // REAL USER ERROR
				sendMeth = hm.name();
				expargs = hm.argCount();
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
		} else if (defn instanceof StructField) {
			dt = ((StructField)defn).type.defn();
		} else if (defn instanceof HandlerLambda) {
			TypedPattern tp = (TypedPattern) ((HandlerLambda)defn).patt;
			dt = tp.type.defn();
		} else if (defn instanceof VarPattern) {
			VarPattern vp = (VarPattern) defn;
			if (vp.type() == null) {
				throw new NotImplementedException("cannot use var " + vp + " as it is not bound to a type");
			} else
				dt = (NamedType) vp.type();
		} else if (defn instanceof ObjectDefn) {
			dt = (ObjectDefn)defn;
		} else if (defn instanceof RequiresContract) {
			dt = ((RequiresContract)defn).actualType();
		} else if (defn instanceof ObjectContract) {
			dt = ((ObjectContract)defn).implementsType().defn();
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
		else if (dt instanceof HandlerImplements)
			this.hi = (HandlerImplements) dt;
		else
			throw new NotImplementedException("cannot handle svc defn of type " + (dt == null ? "NULL" : dt.getClass()));
	}

	@Override
	public void leaveMemberExpr(MemberExpr expr) {
		if (odctor != null) {
			convertObjectCtor(expr);
		} else if (sendMeth != null)
			nv.result(new MakeSend(expr.location(), sendMeth, obj, expargs, handler));
		else
			throw new NotImplementedException("Need to implement the field case");
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
