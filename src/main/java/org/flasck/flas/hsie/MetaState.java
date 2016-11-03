package org.flasck.flas.hsie;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
			LocatedObject lo = convertValue(substs, ae.guard);
			HSIEBlock ifCmd = writeTo.ifCmd(lo.loc, (CreationOfVar) lo.obj);
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
		System.out.println("expr = " + expr.getClass());
		if (!retValues.containsKey(expr))
			throw new UtilException("There is no return value for " + expr);
		LocatedObject lo = retValues.get(expr);
		writeTo.doReturn(lo.loc, lo.obj, closureDependencies(lo.obj));
	}

	public void generateClosure(Map<String, CreationOfVar> substs, Object e) {
		System.out.println("e = " + e.getClass());
		if (!retValues.containsKey(e)) {
			LocatedObject lo = convertValue(substs, e);
			retValues.put(e, new LocatedObject(lo.loc, lo.obj));
		}
	}

	private LocatedObject convertValue(Map<String, CreationOfVar> substs, Object expr) {
		if (expr == null) { // mainly error trapping, but valid in if .. if .. <no else> case
			return new LocatedObject(null, null);
		} else if (expr instanceof NumericLiteral) {
			return new LocatedObject(((NumericLiteral)expr).location, Integer.parseInt(((NumericLiteral)expr).text)); // what about floats?
		} else if (expr instanceof StringLiteral) {
			return new LocatedObject(((StringLiteral)expr).location, expr);
		} else if (expr instanceof FunctionLiteral) {
			return new LocatedObject(((FunctionLiteral)expr).location, expr);
		} else if (expr instanceof TemplateListVar) {
			return new LocatedObject(((TemplateListVar)expr).location, expr);
		} else if (expr instanceof LocalVar) {
			String var = ((LocalVar)expr).uniqueName();
			if (!substs.containsKey(var))
				throw new UtilException("How can this be a local var? " + var + " not in " + substs);
			return new LocatedObject(((LocalVar)expr).varLoc, substs.get(var));
		} else if (expr instanceof IterVar) {
			String var = ((IterVar)expr).var;
			if (!substs.containsKey(var))
				throw new UtilException("How can this be an iter var? " + var + " not in " + substs);
			return new LocatedObject(((IterVar)expr).location, substs.get(var));
		} else if (expr instanceof PackageVar) {
			// a package var is a reference to an absolute something that is referenced by its full scope
			PackageVar pv = (PackageVar)expr;
			return new LocatedObject(pv.location, expr);
		} else if (expr instanceof VarNestedFromOuterFunctionScope) {
			VarNestedFromOuterFunctionScope sv = (VarNestedFromOuterFunctionScope)expr;
			String var = sv.id;
			if (!sv.definedLocally) {
				return new LocatedObject(sv.location, sv);
			}
			if (substs.containsKey(var))
				return new LocatedObject(sv.location, substs.get(var));
			throw new UtilException("Scoped var " + var + " not in " + substs + " for " + form.fnName);
		} else if (expr instanceof ObjectReference || expr instanceof CardFunction) {
			return new LocatedObject(((ExternalRef)expr).location(), expr);
		} else if (expr instanceof CardMember) {
			return new LocatedObject(((CardMember)expr).location(), expr);
		} else if (expr instanceof CardStateRef) {
			return new LocatedObject(((CardStateRef)expr).location(), expr);
		} else if (expr instanceof HandlerLambda) {
			return new LocatedObject(((ExternalRef)expr).location(), expr);
		} else if (expr instanceof ApplyExpr) {
			ApplyExpr e2 = (ApplyExpr) expr;
			List<LocatedObject> ops = new ArrayList<LocatedObject>();
			LocatedObject val = convertValue(substs, e2.fn);
			if (val.obj instanceof CreationOfVar && e2.args.isEmpty()) {
				return val;
			}
			ops.add(val);
			for (Object o : e2.args) {
				ops.add(convertValue(substs, o));
			}
			// TODO: check this doesn't already exist
			ClosureCmd closure = form.createClosure(e2.location);
			List<CreationOfVar> mydeps = new ArrayList<CreationOfVar>();
			for (int i=0;i<ops.size();i++) {
				LocatedObject o = ops.get(i);
				closure.push(o.loc, o.obj);
				if (o.obj instanceof CreationOfVar && closureDepends.containsKey(o.obj) && !mydeps.contains(o.obj)) {
					mydeps.addAll(closureDepends.get(o.obj));
					mydeps.add((CreationOfVar) o.obj);
				}
			}
			closureDepends.put(closure.var, mydeps);
			return new LocatedObject(e2.location, new CreationOfVar(closure.var, e2.location, "clos" + closure.var.idx));
		} else if (expr instanceof CastExpr) {
			CastExpr ce = (CastExpr) expr;
			LocatedObject lo = convertValue(substs, ce.expr);
			CreationOfVar cv = (CreationOfVar) lo.obj;
			HSIEBlock closure = form.getClosure(cv.var);
			closure.downcastType = (Type) ((PackageVar)ce.castTo).defn;
			return lo;
		} else if (expr instanceof TypeCheckMessages) {
			TypeCheckMessages tcm = (TypeCheckMessages) expr;
			LocatedObject lo = convertValue(substs, tcm.expr);
			CreationOfVar cv = (CreationOfVar) lo.obj;
			ClosureCmd closure = form.getClosure(cv.var);
			closure.typecheckMessages = true;
			return lo;
		} else if (expr instanceof AssertTypeExpr) {
			AssertTypeExpr ate = (AssertTypeExpr) expr;
			LocatedObject conv = convertValue(substs, ate.expr);
			if (conv.obj instanceof CreationOfVar) { // it's a closure, delegate to typechecker ..
				CreationOfVar cv = (CreationOfVar) conv.obj;
				ClosureCmd closure = form.getClosure(cv.var);
				closure.assertType = ate.type;
				return conv;
			} else if (conv.obj instanceof StringLiteral) {
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

	public List<SubstExpr> substExprs() {
		return exprs;
	}
}
