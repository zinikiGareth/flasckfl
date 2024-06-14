package org.flasck.flas.method;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.commonBase.ApplyExpr;
import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.commonBase.MemberExpr;
import org.flasck.flas.commonBase.ParenExpr;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.lifting.NestedVarReader;
import org.flasck.flas.parsedForm.AccessorHolder;
import org.flasck.flas.parsedForm.AgentDefinition;
import org.flasck.flas.parsedForm.CardDefinition;
import org.flasck.flas.parsedForm.CastExpr;
import org.flasck.flas.parsedForm.ContractReferencer;
import org.flasck.flas.parsedForm.FieldAccessor;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.IntroduceVar;
import org.flasck.flas.parsedForm.MakeSend;
import org.flasck.flas.parsedForm.ObjectActionHandler;
import org.flasck.flas.parsedForm.ObjectContract;
import org.flasck.flas.parsedForm.ObjectDefn;
import org.flasck.flas.parsedForm.ObjectMethod;
import org.flasck.flas.parsedForm.RequiresContract;
import org.flasck.flas.parsedForm.RequiresHolder;
import org.flasck.flas.parsedForm.StateHolder;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.StructField;
import org.flasck.flas.parsedForm.TemplateBindingOption;
import org.flasck.flas.parsedForm.TemplateNestedField;
import org.flasck.flas.parsedForm.TemplateStylingOption;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.parsedForm.TypedPattern;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.parsedForm.VarPattern;
import org.flasck.flas.parsedForm.assembly.ApplicationRouting;
import org.flasck.flas.parsedForm.assembly.ApplicationRouting.CardBinding;
import org.flasck.flas.parsedForm.ut.UnitTestAssert;
import org.flasck.flas.parsedForm.ut.UnitTestIdentical;
import org.flasck.flas.parser.ut.UnitDataDeclaration;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.LoadBuiltins;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.repository.RepositoryEntry;
import org.flasck.flas.repository.RepositoryReader;
import org.flasck.flas.tc3.NamedType;
import org.flasck.flas.tc3.PolyInstance;
import org.flasck.flas.tc3.Type;
import org.zinutils.exceptions.CantHappenException;
import org.zinutils.exceptions.HaventConsideredThisException;
import org.zinutils.exceptions.NotImplementedException;

public class AccessorConvertor extends LeafAdapter {
	private final NestedVisitor sv;
	private final ErrorReporter errors;
	private final RepositoryReader repository;
	private ContractReferencer cr = null;

	public AccessorConvertor(NestedVisitor sv, ErrorReporter errors, RepositoryReader repository, StateHolder stateHolder) {
		this.sv = sv;
		this.errors = errors;
		this.repository = repository;
		if (stateHolder instanceof ContractReferencer)
			cr = (ContractReferencer)stateHolder;
		sv.push(this);
	}

	@Override
	public void leaveFunction(FunctionDefinition a) {
		sv.result(null);
	}
		
	@Override
	public void postUnitTestAssert(UnitTestAssert a) {
		sv.result(null);
	}
	
	@Override
	public void postUnitTestIdentical(UnitTestIdentical a) {
		sv.result(null);
	}

	@Override
	public void leaveTemplateBindingOption(TemplateBindingOption option) {
		sv.result(null);
	}

	@Override
	public void leaveTemplateStyling(TemplateStylingOption option) {
		sv.result(null);
	}

	@Override
	public boolean visitMemberExpr(MemberExpr expr, int nargs) {
		RepositoryEntry defn;
		if (expr.boundEarly()) {
			defn = expr.defn();
			if (defn instanceof FunctionDefinition || defn instanceof StructDefn) {
				UnresolvedVar uv = new UnresolvedVar(expr.location, "expr");
				uv.bind(defn);
				expr.conversion(uv);
			} else
				throw new HaventConsideredThisException("was not a function");
			return true;
		}
		return false;
	}
	

	@Override
	public void leaveMemberExpr(MemberExpr expr, boolean done) {
		if (done)
			return;
		Expr from = expr.from;
		while (from instanceof ParenExpr)
			from = (Expr) ((ParenExpr)from).expr;
		RepositoryEntry defn;

		if (from instanceof UnresolvedVar) {
			UnresolvedVar uv = (UnresolvedVar) from;
			defn = uv.defn();
		} else if (from instanceof TypeReference) {
			TypeReference uv = (TypeReference) from;
			defn = (RepositoryEntry) uv.namedDefn();
		} else if (from instanceof MemberExpr) {
			MemberExpr me = (MemberExpr) from;
			defn = me.defn();
		} else if (from instanceof ApplyExpr) { // and possibly other cases ...
			defn = (RepositoryEntry) expr.containerType(); // the TypeChecker figured out what the containing type is already
		} else if (from instanceof CastExpr) {
			defn = (RepositoryEntry) ((CastExpr)from).type.namedDefn();
		} else
			throw new NotImplementedException("cannot handle member of " + from.getClass());
		if (defn == null) {
			throw new CantHappenException("defn is null from " + from + " " + from.getClass());
		}
//		List<Type> polys;
		if (defn instanceof PolyInstance) {
			PolyInstance pi = (PolyInstance) defn;
			defn = (RepositoryEntry) pi.struct();
			// I feel we should need this in order to reconstruct the actual type later ...
//			polys = pi.getPolys();
		}
		UnresolvedVar meth = (UnresolvedVar) expr.fld;
		AccessorHolder ah;
		if (defn instanceof IntroduceVar) {
			defn = (RepositoryEntry) ((IntroduceVar)defn).introducedAs();
			if (defn == null)
				throw new CantHappenException("defn is null from " + defn);
		}
		if (defn instanceof UnitDataDeclaration) {
			UnitDataDeclaration udd = (UnitDataDeclaration) defn;
			NamedType td = udd.ofType.namedDefn();
			if (td instanceof StateHolder) {
				// UDDs can prod state directly on cards, agents and objects ...
				StateHolder sh = (StateHolder)td;
				if (sh.state() != null && sh.state().hasMember(meth.var)) {
					expr.conversion(new ApplyExpr(expr.location, LoadBuiltins.probeState, expr.from, new StringLiteral(meth.location, meth.var)));
					return;
				}
			}
			RepositoryEntry entry = repository.get(FunctionName.function(meth.location, td.name(), meth.var).uniqueName());
			if (entry != null && entry instanceof FunctionDefinition) {
				UnresolvedVar call = new UnresolvedVar(meth.location, meth.var);
				call.bind(entry);
				expr.conversion(new ApplyExpr(expr.location, call, new ApplyExpr(expr.location, LoadBuiltins.getUnderlying, from)));
				return;
			}

			if (td instanceof AccessorHolder && ((AccessorHolder)td).getAccessor(meth.var) != null) 
				ah = (AccessorHolder) td;
			else if (td instanceof ObjectDefn && ((ObjectDefn)td).getMethod(meth.var) != null) {
				ObjectDefn od = (ObjectDefn) td;
				ObjectMethod m = od.getMethod(meth.var);
				expr.conversion(new MakeSend(expr.location(), m.name(), from, m.argCount()));
				return;
			} else {
				errors.message(meth.location, "there is no suitable value for '" + meth.var + "' on " + td.name().uniqueName());
				return;
			}
		} else if (defn instanceof StructDefn) {
			ah = (AccessorHolder) defn;
		} else if (defn instanceof ObjectDefn) {
			// it's actually a ctor not an accessor
			ObjectDefn od = (ObjectDefn) defn;
			ObjectActionHandler odctor = od.getConstructor(meth.var);
			if (odctor == null)
				throw new CantHappenException("no constructor " + meth.var);
			convertObjectCtor(od, odctor, expr);
//			UnresolvedVar cv = new UnresolvedVar(from.location(), meth.var);
//			cv.bind(odctor);
//			expr.conversion(cv);
			return;
		} else if (defn instanceof TypedPattern) {
			TypedPattern tp = (TypedPattern)defn;
			Type ty = tp.type();
			if (ty instanceof PolyInstance)
				ty = ((PolyInstance)ty).struct(); 
			ah = (AccessorHolder) ty;
		} else if (defn instanceof VarPattern) {
			Type ty = ((VarPattern)defn).type();
			if (ty == null) {
				throw new CantHappenException("type of " + defn + " has not been bound");
			}
			if (ty instanceof PolyInstance)
				ty = ((PolyInstance)ty).struct();
			if (ty instanceof AccessorHolder)
				ah = (AccessorHolder) ty;
			else {
				errors.message(meth.location, "cannot access members of " + ty.signature());
				return;
			}
		} else if (defn instanceof FunctionDefinition) {
			FunctionDefinition fn = (FunctionDefinition) defn;
			if (fn.argCountWithoutHolder() == 0) {
				if (fn.hasState())
					ah = (AccessorHolder) fn.type().get(1);
				else
					ah = (AccessorHolder) fn.type();
			} else {
				NestedVarReader nv = fn.nestedVars();
				if (nv.patterns().size() == fn.argCountWithoutHolder()) {
					// It feels like we need to apply the function to these arguments, but that isn't actually true, since it is done automatically
					// during code generation
					ah = (AccessorHolder) fn.type().get(nv.size() + (fn.hasState()?1:0));
				} else
					throw new NotImplementedException("cannot extract object from " + defn.getClass() + " with " + fn.argCount());
			}
		} else if (defn instanceof StructField) {
			ah = (AccessorHolder) ((StructField)defn).type();
		} else if (defn instanceof TemplateNestedField) {
			ah = (AccessorHolder) ((TemplateNestedField)defn).type();
		} else if (defn instanceof CardBinding) {
			StateHolder sh = ((CardBinding)defn).type();
			if (sh.state() != null && sh.state().hasMember(meth.var)) {
				expr.conversion(new ApplyExpr(expr.location, LoadBuiltins.probeState, expr.from, new StringLiteral(meth.location, meth.var)));
				return;
			} else {
				errors.message(meth.location, "there is no suitable value for '" + meth.var + "' on " + sh.name().uniqueName());
				return;
			}
		} else if (defn instanceof ApplicationRouting) {
//			ApplicationRouting ar = (ApplicationRouting) defn;
//			CardBinding cb = ar.getCard(meth.var);
			expr.conversion(new ApplyExpr(expr.location, LoadBuiltins.probeState, expr.from, new StringLiteral(meth.location, meth.var)));
			return;
		} else
			throw new NotImplementedException("cannot extract object from " + defn.getClass());
		FieldAccessor acc = ah.getAccessor(meth.var);
		if (acc == null)
			errors.message(meth.location, "there is no accessor '" + meth.var + "' on " + ah.name().uniqueName());
		else {
			expr.conversion(acc.acor(expr.from));
			if (expr.defn() == null)
				expr.bind((RepositoryEntry) acc, false);
		}
	}
	
	// basically copied (and adapted) from MemberExprConvertor.  It would be good to share :)
	// but beware the difference in usage
	private void convertObjectCtor(ObjectDefn od, ObjectActionHandler odctor, MemberExpr expr) {
		UnresolvedVar fn = new UnresolvedVar(expr.fld.location(), ((UnresolvedVar)expr.fld).var);
		fn.bind(odctor);
		List<Expr> args = new ArrayList<>();
		for (ObjectContract oc : od.contracts) {
			if (cr != null) {
				NamedType parent = cr.getParent();
				if (parent instanceof AgentDefinition || parent instanceof CardDefinition) {
					RequiresHolder ad = (RequiresHolder) parent;
					RequiresContract found = null;
					for (RequiresContract rc : ad.requires()) {
						if (rc.actualType() == oc.implementsType().namedDefn()) {
							found = rc;
							break;
						}
					}
					if (found == null) {
						errors.message(expr.fld.location(), "there is no available implementation of " + oc.implementsType().namedDefn().name().uniqueName());
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
			expr.conversion(fn);
		else
			expr.conversion(new ApplyExpr(expr.location, fn, (Object[])args.toArray(new Expr[args.size()])));
	}
}
