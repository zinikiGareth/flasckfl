package org.flasck.flas.hsie;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.ApplyExpr;
import org.flasck.flas.commonBase.CastExpr;
import org.flasck.flas.commonBase.IfExpr;
import org.flasck.flas.commonBase.LocatedObject;
import org.flasck.flas.commonBase.NumericLiteral;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.commonBase.template.TemplateListVar;
import org.flasck.flas.errors.ErrorResult;
import org.flasck.flas.rewrittenForm.AssertTypeExpr;
import org.flasck.flas.rewrittenForm.CardFunction;
import org.flasck.flas.rewrittenForm.CardMember;
import org.flasck.flas.rewrittenForm.CardStateRef;
import org.flasck.flas.rewrittenForm.ExternalRef;
import org.flasck.flas.rewrittenForm.FunctionLiteral;
import org.flasck.flas.rewrittenForm.HandlerLambda;
import org.flasck.flas.rewrittenForm.IterVar;
import org.flasck.flas.rewrittenForm.LocalVar;
import org.flasck.flas.rewrittenForm.ObjectReference;
import org.flasck.flas.rewrittenForm.PackageVar;
import org.flasck.flas.rewrittenForm.TypeCheckMessages;
import org.flasck.flas.rewrittenForm.VarNestedFromOuterFunctionScope;
import org.flasck.flas.typechecker.Type;
import org.flasck.flas.vcode.hsieForm.ClosureCmd;
import org.flasck.flas.vcode.hsieForm.CreationOfVar;
import org.flasck.flas.vcode.hsieForm.HSIEBlock;
import org.flasck.flas.vcode.hsieForm.HSIEForm;
import org.flasck.flas.vcode.hsieForm.Var;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zinutils.exceptions.UtilException;

public class MetaState {
	static final Logger logger = LoggerFactory.getLogger("HSIE");

	private final ErrorResult errors;
	public final HSIEForm form;
	private final List<SubstExpr> exprs = new ArrayList<SubstExpr>();
	final List<State> allStates = new ArrayList<State>();
	private final Map<Var, Map<String, Var>> fieldVars = new HashMap<Var, Map<String, Var>>();
	private final Map<Object, LocatedObject> retValues = new HashMap<Object, LocatedObject>();
	private final Map<Var, List<CreationOfVar>> closureDepends = new HashMap<Var, List<CreationOfVar>>();

	public MetaState(ErrorResult errors, HSIEForm form) {
		this.errors = errors;
		this.form = form;
	}

	public void add(State s) {
		allStates.add(s);
	}

	public boolean allDone() {
		return allStates.isEmpty();
	}

	public State first() {
		return allStates.remove(0);
	}

	public Var allocateVar() {
		return form.allocateVar();
	}

	public Var varFor(Var from, String field) {
		if (!fieldVars.containsKey(from))
			fieldVars.put(from, new HashMap<String, Var>());
		if (!fieldVars.get(from).containsKey(field))
			fieldVars.get(from).put(field, allocateVar());
		Var ret = fieldVars.get(from).get(field);
//		System.out.println("Allocating " + ret + " for " + from + "." + field);
		return ret;
	}

	public void addExpr(SubstExpr ex) {
		exprs.add(ex);
	}

	public void requireClosure(Var var) {
		closureDepends.put(var, new ArrayList<CreationOfVar>());
	}

	public void mapVar(String id, CreationOfVar cov) {
		for (SubstExpr se : exprs)
			se.substs.put(id, cov);
	}

	public void writeExpr(SubstExpr se, HSIEBlock writeTo) {
		writeIfExpr(se.substs, se.expr, writeTo);
	}
	
	private void writeIfExpr(Map<String, CreationOfVar> substs, Object expr, HSIEBlock writeTo) {
		logger.info("Handling " + form.fnName + "; expr = " + expr + "; substs = " + substs);
		// First handle the explicit "if" and "let" cases
		if (expr instanceof IfExpr) {
			IfExpr ae = (IfExpr) expr;
			List<InputPosition> elocs = new ArrayList<InputPosition>();
			HSIEBlock ifCmd = writeTo.ifCmd(ae.location(), (CreationOfVar) convertValue(elocs, substs, ae.guard));
			writeIfExpr(substs, ae.ifExpr, ifCmd);
			if (ae.elseExpr != null)
				writeIfExpr(substs, ae.elseExpr, writeTo);
			else
				writeTo.caseError();
			return;
		}
		
		writeFinalExpr(substs, expr, writeTo);
	}

	public void writeFinalExpr(Map<String, CreationOfVar> substs, Object expr, HSIEBlock writeTo) {
		LocatedObject lo = getValueFor(substs, expr);
		writeTo.doReturn(lo.loc, lo.obj, closureDependencies(lo.obj));
	}

	public LocatedObject getValueFor(Map<String, CreationOfVar> substs, Object e) {
		if (!retValues.containsKey(e)) {
			List<InputPosition> elocs = new ArrayList<InputPosition>();
			Object val = convertValue(elocs, substs, e);
			retValues.put(e, new LocatedObject(elocs.get(0), val));
		}
		return retValues.get(e);
	}

	private Object convertValue(List<InputPosition> locs, Map<String, CreationOfVar> substs, Object expr) {
		if (expr == null) { // mainly error trapping, but valid in if .. if .. <no else> case
			locs.add(null);
			return null;
		} else if (expr instanceof NumericLiteral) {
			locs.add(((NumericLiteral)expr).location);
			return Integer.parseInt(((NumericLiteral)expr).text); // what about floats?
		} else if (expr instanceof StringLiteral) {
			locs.add(((StringLiteral)expr).location);
			return expr;
		} else if (expr instanceof FunctionLiteral) {
			locs.add(((FunctionLiteral)expr).location);
			return expr;
		} else if (expr instanceof TemplateListVar) {
			locs.add(((TemplateListVar)expr).location);
			return expr;
		} else if (expr instanceof LocalVar) {
			locs.add(((LocalVar)expr).varLoc);
			String var = ((LocalVar)expr).uniqueName();
			if (!substs.containsKey(var))
				throw new UtilException("How can this be a local var? " + var + " not in " + substs);
			return substs.get(var);
		} else if (expr instanceof IterVar) {
			locs.add(((IterVar)expr).location);
			String var = ((IterVar)expr).var;
			if (!substs.containsKey(var))
				throw new UtilException("How can this be an iter var? " + var + " not in " + substs);
			return substs.get(var);
		} else if (expr instanceof PackageVar) {
			// a package var is a reference to an absolute something that is referenced by its full scope
			PackageVar pv = (PackageVar)expr;
			locs.add(pv.location);
			return expr;
		} else if (expr instanceof VarNestedFromOuterFunctionScope) {
			VarNestedFromOuterFunctionScope sv = (VarNestedFromOuterFunctionScope)expr;
			locs.add(sv.location);
			String var = sv.id;
			if (!sv.definedLocally) {
				return sv;
			}
			if (substs.containsKey(var))
				return substs.get(var);
			throw new UtilException("Scoped var " + var + " not in " + substs + " for " + form.fnName);
		} else if (expr instanceof ObjectReference || expr instanceof CardFunction) {
			locs.add(((ExternalRef)expr).location());
			return expr;
		} else if (expr instanceof CardMember) {
			locs.add(((ExternalRef)expr).location());
			return expr;
		} else if (expr instanceof CardStateRef) {
			locs.add(((CardStateRef)expr).location());
			return expr;
		} else if (expr instanceof HandlerLambda) {
			locs.add(((ExternalRef)expr).location());
			return expr;
		} else if (expr instanceof ApplyExpr) {
			ApplyExpr e2 = (ApplyExpr) expr;
			List<Object> ops = new ArrayList<Object>();
			List<InputPosition> elocs = new ArrayList<InputPosition>();
			Object val = convertValue(elocs, substs, e2.fn);
			if (val instanceof CreationOfVar && e2.args.isEmpty()) {
				locs.add(e2.location);
				return val;
			}
			ops.add(val);
			for (Object o : e2.args)
				ops.add(convertValue(elocs, substs, o));
			// TODO: check this doesn't already exist
			ClosureCmd closure = form.createClosure(e2.location);
			List<CreationOfVar> mydeps = new ArrayList<CreationOfVar>();
			if (ops.size() != elocs.size())
				throw new UtilException("Misplaced location or operation: " +  elocs.size() + " != " + ops.size());
			for (int i=0;i<ops.size();i++) {
				Object o = ops.get(i);
				if (elocs.get(i) == null) {
					System.out.println("Failed to find location for " + i + " in:");
					System.out.println("   -> " + e2);
					System.out.println("   -> " + ops);
					System.out.println("   -> " + elocs);
				}
				closure.push(elocs.get(i), o);
				if (o instanceof CreationOfVar && closureDepends.containsKey(o) && !mydeps.contains(o)) {
					mydeps.addAll(closureDepends.get(o));
					mydeps.add((CreationOfVar) o);
				}
			}
			locs.add(e2.location);
			closureDepends.put(closure.var, mydeps);
			return new CreationOfVar(closure.var, e2.location, "clos" + closure.var.idx);
		} else if (expr instanceof CastExpr) {
			CastExpr ce = (CastExpr) expr;
			CreationOfVar cv = (CreationOfVar) convertValue(locs, substs, ce.expr);
			HSIEBlock closure = form.getClosure(cv.var);
			closure.downcastType = (Type) ((PackageVar)ce.castTo).defn;
			return cv;
		} else if (expr instanceof TypeCheckMessages) {
			TypeCheckMessages tcm = (TypeCheckMessages) expr;
			CreationOfVar cv = (CreationOfVar) convertValue(locs, substs, tcm.expr);
			ClosureCmd closure = form.getClosure(cv.var);
			closure.typecheckMessages = true;
			return cv;
		} else if (expr instanceof AssertTypeExpr) {
			AssertTypeExpr ate = (AssertTypeExpr) expr;
			Object conv = convertValue(locs, substs, ate.expr);
			if (conv instanceof CreationOfVar) { // it's a closure, delegate to typechecker ..
				CreationOfVar cv = (CreationOfVar) conv;
				ClosureCmd closure = form.getClosure(cv.var);
				closure.assertType = ate.type;
				return cv;
			} else if (conv instanceof StringLiteral) {
				if (!ate.type.name().equals("String")) {
					errors.message(ate.location(), "cannot assign a string to " + ate.type.name());
				}
				return conv;
			} else
				throw new UtilException("We should check " + conv + " against " + ate.type);
		} else {
			System.out.println("HSIE Cannot Handle: " + expr);
			throw new UtilException("HSIE Cannot handle " + expr + " " + (expr != null? " of type " + expr.getClass() : ""));
		}
	}

	public List<CreationOfVar> closureDependencies(Object var) {
		List<CreationOfVar> ret = new ArrayList<CreationOfVar>();
		closeDependencies(ret, var);
		return ret;
	}

	private void closeDependencies(List<CreationOfVar> ret, Object var) {
		List<CreationOfVar> more = null;
		if (var instanceof Var)
			more = closureDepends.get(var);
		else if (var instanceof CreationOfVar)
			more = closureDepends.get(((CreationOfVar)var).var);

		if (more == null)
			return;
		
		for (CreationOfVar cv : more)
			if (!ret.contains(cv)) {
				closeDependencies(ret, cv);
				if (ret.contains(cv))
					throw new UtilException("I suspect this is a cycle");
				ret.add(cv);
			}
	}

	public Object getSubst(String uniqueName) {
		for (SubstExpr se : exprs) {
			if (se.substs.containsKey(uniqueName))
				return se.substs.get(uniqueName);
		}
		throw new UtilException("There is no var for " + uniqueName);
	}

	public void dependency(ClosureCmd clos, CreationOfVar cov) {
		closureDepends.get(clos.var).add(cov);
	}
}
