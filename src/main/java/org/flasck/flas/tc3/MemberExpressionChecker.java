package org.flasck.flas.tc3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.commonBase.MemberExpr;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.compiler.DeferMeException;
import org.flasck.flas.compiler.UnboundTypeException;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.AgentDefinition;
import org.flasck.flas.parsedForm.CardDefinition;
import org.flasck.flas.parsedForm.ContractDecl;
import org.flasck.flas.parsedForm.ContractMethodDecl;
import org.flasck.flas.parsedForm.FieldAccessor;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.ObjectCtor;
import org.flasck.flas.parsedForm.ObjectDefn;
import org.flasck.flas.parsedForm.ObjectMethod;
import org.flasck.flas.parsedForm.PolyHolder;
import org.flasck.flas.parsedForm.PolyType;
import org.flasck.flas.parsedForm.StateHolder;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.StructField;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.parser.ut.UnitDataDeclaration;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.repository.RepositoryEntry;
import org.flasck.flas.repository.RepositoryReader;
import org.flasck.flas.repository.ResultAware;
import org.flasck.flas.tc3.ExpressionChecker.ExprResult;
import org.zinutils.exceptions.CantHappenException;
import org.zinutils.exceptions.HaventConsideredThisException;
import org.zinutils.exceptions.NotImplementedException;

public class MemberExpressionChecker extends LeafAdapter implements ResultAware {
	private final ErrorReporter errors;
	private final RepositoryReader repository;
	private final NestedVisitor nv;
	private final List<Type> results = new ArrayList<>();
	private final CurrentTCState state;
	private boolean inTemplate;

	public MemberExpressionChecker(ErrorReporter errors, RepositoryReader repository, CurrentTCState state, NestedVisitor nv, boolean inTemplate) {
		this.errors = errors;
		this.repository = repository;
		this.state = state;
		this.nv = nv;
		this.inTemplate = inTemplate;
	}
	
	@Override
	public void visitExpr(Expr expr, int nArgs) {
		nv.push(new ExpressionChecker(errors, repository, state, nv, inTemplate));
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
		List<Type> polys = null;
		if (ty instanceof PolyInstance) {
			PolyInstance pi = (PolyInstance) ty;
			polys = pi.getPolys();
			ty = pi.struct();
		}
		// TODO: can I move this to resolver?
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
				errors.message(fld.location(), "there is no field '" + fld.var + "' in " + sd.name().uniqueName());
				nv.result(new ErrorType());
			} else {
				if (polys != null)
					nv.result(processPolys(polys, sd, sf.type.defn()));
				else
					nv.result(sf.type.defn());
			}
		} else if (ty instanceof ObjectDefn) {
			ObjectDefn od = (ObjectDefn) ty;
			FieldAccessor fa = od.getAccessor(fld.var);
			if (fa != null) {
				try {
					nv.result(fa.type());
				} catch (UnboundTypeException ute) {
					throw new DeferMeException();
				}
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
			if (expr.from instanceof UnresolvedVar && ((UnresolvedVar)expr.from).defn() instanceof UnitDataDeclaration) {
				handleStateHolderUDD((StateHolder) ty, fld.location, fld.var);
				return;
			}
			
			errors.message(expr.fld.location(), "object " + od.name() + " does not have a method, ctor or acor " + fld.var);
			nv.result(new ErrorType());
		} else if (ty instanceof CardDefinition || ty instanceof AgentDefinition) {
			if (expr.from instanceof UnresolvedVar && ((UnresolvedVar)expr.from).defn() instanceof UnitDataDeclaration)
				handleStateHolderUDD((StateHolder) ty, fld.location, fld.var);
			else {
				errors.message(fld.location(), "there is insufficient information to deduce the type of the object in order to apply it to '" + fld.var + "'");
				nv.result(new ErrorType());
			}
		} else if (expr.from instanceof UnresolvedVar) {
			UnresolvedVar var = (UnresolvedVar) expr.from;
			errors.message(var.location(), "there is insufficient information to deduce the type of '" + var.var + "' in order to apply it to '" + fld.var + "'");
			nv.result(new ErrorType());
		} else
			throw new NotImplementedException("Not yet handled: " + ty);
	}

	private Type processPolys(List<Type> polys, PolyHolder ph, Type found) {
		if (found instanceof PolyInstance) {
			PolyInstance pi = (PolyInstance) found;
			List<Type> mapped = new ArrayList<>();
			for (Type pt : pi.getPolys()) {
				mapped.add(processPolys(polys, (PolyHolder)pi.struct(), pt));
			}
			return new PolyInstance(pi.location(), pi.struct(), mapped);
		} else if (found instanceof PolyType) {
			Iterator<PolyType> ipt = ph.polys().iterator();
			Iterator<Type> it = polys.iterator();
			while (ipt.hasNext()) {
				Type ret = it.next();
				if (ipt.next() == found)
					return ret;
			}
			throw new CantHappenException("the poly type was not in the list");
		} else
			return found;
	}

	private void handleStateHolderUDD(StateHolder ty, InputPosition loc, String var) {
		if (ty.state().hasMember(var)) {
			nv.result(ty.state().findField(var).type.defn());
		} else {
			RepositoryEntry entry = repository.get(FunctionName.function(loc, ty.name(), var).uniqueName());
			if (entry != null && entry instanceof FunctionDefinition) {
				Type type = ((FunctionDefinition)entry).type();
				// This should be an Apply and the first arg should match ty and we should return the rest
				if (!(type instanceof Apply))
					throw new HaventConsideredThisException("I would expect this to be an Apply with 'ty' as the first arg");
				Apply app = (Apply) type;
				nv.result(app.appliedTo(ty));
			} else {
				errors.message(loc, "there is no state member or function '" + var + "' in " + ty.name().uniqueName());
				nv.result(new ErrorType());
				return;
			}
		}
	}
}
