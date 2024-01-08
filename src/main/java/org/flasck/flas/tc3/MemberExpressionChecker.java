package org.flasck.flas.tc3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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
import org.flasck.flas.parsedForm.HandlerImplements;
import org.flasck.flas.parsedForm.ObjectActionHandler;
import org.flasck.flas.parsedForm.ObjectDefn;
import org.flasck.flas.parsedForm.ObjectMethod;
import org.flasck.flas.parsedForm.PolyHolder;
import org.flasck.flas.parsedForm.PolyType;
import org.flasck.flas.parsedForm.StateHolder;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.StructField;
import org.flasck.flas.parsedForm.UnionTypeDefn;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.parsedForm.assembly.ApplicationRouting;
import org.flasck.flas.parser.ut.UnitDataDeclaration;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.LoadBuiltins;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.repository.RepositoryEntry;
import org.flasck.flas.repository.RepositoryReader;
import org.flasck.flas.repository.ResultAware;
import org.flasck.flas.tc3.ExpressionChecker.ExprResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zinutils.exceptions.CantHappenException;
import org.zinutils.exceptions.HaventConsideredThisException;
import org.zinutils.exceptions.NotImplementedException;

public class MemberExpressionChecker extends LeafAdapter implements ResultAware {
	public final static Logger logger = LoggerFactory.getLogger("TypeChecker");
	private final ErrorReporter errors;
	private final RepositoryReader repository;
	private final NestedVisitor nv;
	private final String fnCxt;
	private final List<Type> results = new ArrayList<>();
	private final CurrentTCState state;
	private boolean inTemplate;

	public MemberExpressionChecker(ErrorReporter errors, RepositoryReader repository, CurrentTCState state, NestedVisitor nv, String fnCxt, boolean inTemplate) {
		this.errors = errors;
		this.repository = repository;
		this.state = state;
		this.nv = nv;
		this.fnCxt = fnCxt;
		this.inTemplate = inTemplate;
	}
	
	@Override
	public void visitExpr(Expr expr, int nArgs) {
		nv.push(new ExpressionChecker(errors, repository, state, nv, fnCxt, inTemplate));
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
		expr.bindContainerType(ty);
		UnresolvedVar fld = (UnresolvedVar)expr.fld;
		List<Type> polys = null;
		if (ty instanceof PolyInstance) {
			PolyInstance pi = (PolyInstance) ty;
			polys = pi.polys();
			ty = pi.struct();
		}
		// TODO: can I move this to resolver?
		if (ty instanceof ErrorType) {
			announce(expr, ty);
		} else if (ty instanceof ContractDecl) {
			figureContractMethod(expr, (ContractDecl) ty, fld);
		} else if (ty instanceof HandlerImplements) {
			figureContractMethod(expr, ((HandlerImplements) ty).actualType(), fld);
		} else if (ty instanceof StructDefn) {
			StructDefn sd = (StructDefn) ty;
			StructField sf = sd.findField(fld.var);
			if (sf == null) {
				errors.message(fld.location(), "there is no field '" + fld.var + "' in " + sd.name().uniqueName());
				announce(expr, new ErrorType());
			} else {
				if (polys != null) {
					Map<String, Type> mapping = new HashMap<>();
					for (int i=0;i<polys.size();i++) {
						mapping.put(sd.polys().get(i).shortName(), polys.get(i));
					}
					announce(expr, replacePolyVarsWithDeducedTypes(mapping, sf.type.namedDefn()));
				} else
					announce(expr, sf.type.namedDefn());
			}
		} else if (ty instanceof ObjectDefn) {
			ObjectDefn od = (ObjectDefn) ty;
			FieldAccessor fa = od.getAccessor(fld.var);
			if (fa != null) {
				try {
					announce(expr, fa.type());
				} catch (UnboundTypeException ute) {
					logger.info("type for " + fa + " is unbound, deferring");
					throw new DeferMeException();
				}
				return;
			}
			ObjectActionHandler ctor = od.getConstructor(fld.var);
			if (ctor != null) {
				announce(expr, ctor.resolveType(repository));
				return;
			}
			ObjectMethod meth = od.getMethod(fld.var);
			if (meth != null) {
				announce(expr, meth.type());
				return;
			}
			if (expr.from instanceof UnresolvedVar && ((UnresolvedVar)expr.from).defn() instanceof UnitDataDeclaration) {
				handleStateHolderUDD(expr, (StateHolder) ty, fld.location, fld.var);
				return;
			}
			
			errors.message(expr.fld.location(), "object " + od.name() + " does not have a method, ctor or acor " + fld.var);
			announce(expr, new ErrorType());
		} else if (ty instanceof ApplicationRouting) {
			announce(expr, new ErrorType());
		} else if (ty instanceof CardDefinition || ty instanceof AgentDefinition) {
			if (expr.from instanceof UnresolvedVar && ((UnresolvedVar)expr.from).defn() instanceof UnitDataDeclaration)
				handleStateHolderUDD(expr, (StateHolder) ty, fld.location, fld.var);
			else {
				errors.message(fld.location(), "there is insufficient information to deduce the type of the object in order to apply it to '" + fld.var + "'");
				announce(expr, new ErrorType());
			}
		} else if (ty instanceof UnifiableType) {
			// It's going to be hard to figure this out, but we need to have a special rule for dealing with it and then unify later
			UnifiableType container = (UnifiableType) ty;
			UnifiableType ut = state.createUT(expr.location(), "memberExpr for " + expr.toString());
			ut.isFieldOf(expr, container, fld.var);
			announce(expr, ut);
		} else if (ty instanceof UnionTypeDefn) {
			errors.message(expr.fld.location(), "cannot access members of unions");
			announce(expr, new ErrorType());
		} else if (ty instanceof Primitive && ty.signature().equals("Entity") && fld.var.equals("id")) {
			announce(expr, LoadBuiltins.string);
		} else if (ty instanceof Primitive) {
			errors.message(expr.fld.location(), ty.signature() + " does not have members");
			announce(expr, new ErrorType());
		} else
			throw new NotImplementedException("Not yet handled: " + ty);
	}

	private void figureContractMethod(MemberExpr expr, ContractDecl cd, UnresolvedVar fld) {
		ContractMethodDecl method = cd.getMethod(fld.var);
		if (method == null) {
			errors.message(fld.location(), "there is no method '" + fld.var + "' in " + cd.name().uniqueName());
			announce(expr, new ErrorType());
		} else {
			announce(expr, method.type());
			expr.bindContractMethod(method);
		}
	}
	
	private void announce(MemberExpr me, Type ty) {
		me.bindContainedType(ty);
		nv.result(ty);
	}

	private Type replacePolyVarsWithDeducedTypes(Map<String, Type> mapping, Type found) {
		if (found instanceof PolyInstance) {
			PolyInstance pi = (PolyInstance) found;
			Map<String, Type> remapped = new HashMap<>();
			Iterator<PolyType> ipt = ((PolyHolder)pi.struct()).polys().iterator();
			Iterator<Type> it = pi.polys().iterator();
			while (ipt.hasNext()) {
				remapped.put(ipt.next().shortName(), mapping.get(((PolyType)it.next()).shortName()));
			}
			List<Type> mapped = new ArrayList<>();
			for (PolyType pt : ((PolyHolder)pi.struct()).polys()) {
				mapped.add(replacePolyVarsWithDeducedTypes(remapped, pt));
			}
			return new PolyInstance(pi.location(), pi.struct(), mapped);
		} else if (found instanceof PolyType) {
			PolyType pt = (PolyType) found;
			if (mapping.containsKey(pt.shortName()))
				return mapping.get(pt.shortName());
			else
				throw new CantHappenException("the poly type was not in the list");
		} else
			return found;
	}

	private void handleStateHolderUDD(MemberExpr me, StateHolder ty, InputPosition loc, String var) {
		if (ty.state().hasMember(var)) {
			announce(me, ty.state().findField(var).type.namedDefn());
		} else {
			RepositoryEntry entry = repository.get(FunctionName.function(loc, ty.name(), var).uniqueName());
			if (entry != null && entry instanceof FunctionDefinition) {
				Type type = ((FunctionDefinition)entry).type();
				// This should be an Apply and the first arg should match ty and we should return the rest
				if (!(type instanceof Apply))
					throw new HaventConsideredThisException("I would expect this to be an Apply with 'ty' as the first arg");
				Apply app = (Apply) type;
				announce(me, app.appliedTo(ty));
			} else {
				errors.message(loc, "there is no state member or function '" + var + "' in " + ty.name().uniqueName());
				announce(me, new ErrorType());
				return;
			}
		}
	}
}
