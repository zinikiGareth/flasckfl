package org.flasck.flas.hsie;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.parsedForm.AbsoluteVar;
import org.flasck.flas.parsedForm.ApplyExpr;
import org.flasck.flas.parsedForm.CardFunction;
import org.flasck.flas.parsedForm.CardMember;
import org.flasck.flas.parsedForm.CardStateRef;
import org.flasck.flas.parsedForm.CastExpr;
import org.flasck.flas.parsedForm.ExternalRef;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.FunctionLiteral;
import org.flasck.flas.parsedForm.HandlerLambda;
import org.flasck.flas.parsedForm.IfExpr;
import org.flasck.flas.parsedForm.IterVar;
import org.flasck.flas.parsedForm.LetExpr;
import org.flasck.flas.parsedForm.LocalVar;
import org.flasck.flas.parsedForm.MethodDefinition;
import org.flasck.flas.parsedForm.NumericLiteral;
import org.flasck.flas.parsedForm.ObjectReference;
import org.flasck.flas.parsedForm.StringLiteral;
import org.flasck.flas.parsedForm.TemplateListVar;
import org.flasck.flas.typechecker.Type;
import org.flasck.flas.vcode.hsieForm.CreationOfVar;
import org.flasck.flas.vcode.hsieForm.HSIEBlock;
import org.flasck.flas.vcode.hsieForm.HSIEForm;
import org.flasck.flas.vcode.hsieForm.PushCmd;
import org.flasck.flas.vcode.hsieForm.Var;
import org.zinutils.exceptions.UtilException;

public class MetaState {
	public class LocatedObject {
		InputPosition loc;
		Object obj;

		public LocatedObject(InputPosition loc, Object obj) {
			this.loc = loc;
			this.obj = obj;
		}
	}

	public final HSIEForm form;
	final List<State> allStates = new ArrayList<State>();
	private final Map<Var, Map<String, Var>> fieldVars = new HashMap<Var, Map<String, Var>>();
	private final Map<Object, LocatedObject> retValues = new HashMap<Object, LocatedObject>();
	private final Map<Var, List<CreationOfVar>> closureDepends = new HashMap<Var, List<CreationOfVar>>();

	public MetaState(HSIEForm form) {
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

	public void writeExpr(SubstExpr se, HSIEBlock writeTo) {
		writeIfExpr(se.substs, se.expr, writeTo);
	}
	
	private void writeIfExpr(Map<String, CreationOfVar> substs, Object expr, HSIEBlock writeTo) {
		if (expr instanceof IfExpr) {
			IfExpr ae = (IfExpr) expr;
			List<InputPosition> elocs = new ArrayList<InputPosition>();
			HSIEBlock ifCmd = writeTo.ifCmd((CreationOfVar) convertValue(elocs, substs, ae.guard));
			writeIfExpr(substs, ae.ifExpr, ifCmd);
			if (ae.elseExpr != null)
				writeIfExpr(substs, ae.elseExpr, writeTo);
			else
				writeTo.caseError();
			return;
		} else if (expr instanceof LetExpr) {
			LetExpr let = (LetExpr) expr;
			LocatedObject lo = getValueFor(substs, let.val);
			CreationOfVar var;
			if (lo.obj instanceof CreationOfVar) {
				var = (CreationOfVar) lo.obj;
				var = new CreationOfVar(var.var, lo.loc, let.var);
			} else {
				Var v = allocateVar();
				var = new CreationOfVar(v, null, let.var);
				HSIEBlock closure = form.closure(v);
				closure.push(lo.loc, lo.obj);
			}
			substs.put(let.var, var);
			writeIfExpr(substs, let.expr, writeTo);
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
			String var = ((LocalVar)expr).var;
			if (!substs.containsKey(var))
				throw new UtilException("How can this be a local var? " + var + " not in " + substs);
			return substs.get(var);
		} else if (expr instanceof IterVar) {
			locs.add(((IterVar)expr).location);
			String var = ((IterVar)expr).var;
			if (!substs.containsKey(var))
				throw new UtilException("How can this be a iter var? " + var + " not in " + substs);
			return substs.get(var);
		} else if (expr instanceof AbsoluteVar) {
			AbsoluteVar av = (AbsoluteVar)expr;
			locs.add(av.location);
			form.dependsOn(av);
			// If we are calling a nested function or method, make sure to record the variables
			// that it "assumes" are in its scope so that we can include them in later calls if needed
			if ((av.defn instanceof MethodDefinition || av.defn instanceof FunctionDefinition) && av.id.startsWith(form.fnName + "_")) {
				Var var = allocateVar();
				HSIEBlock closure = form.closure(var);
				PushCmd pc = closure.push(av.location, av);
				for (int j=0;j<form.nformal;j++)
					pc.inheritArgs.add(form.vars.get(j));
				closureDepends.put(var, new ArrayList<CreationOfVar>());
				return new CreationOfVar(var, av.location, "clos" + var.idx);
			} else
				return expr;
		} else if (expr instanceof ObjectReference || expr instanceof CardFunction) {
			locs.add(((ExternalRef)expr).location());
			form.dependsOn(expr);
			return expr;
		} else if (expr instanceof CardMember) {
			locs.add(((ExternalRef)expr).location());
			form.dependsOn(expr);
			return expr;
		} else if (expr instanceof CardStateRef) {
			locs.add(((CardStateRef)expr).location());
			return expr;
		} else if (expr instanceof HandlerLambda) {
			locs.add(((ExternalRef)expr).location());
			form.dependsOn(expr);
			return expr;
		} else if (expr instanceof ApplyExpr) {
			ApplyExpr e2 = (ApplyExpr) expr;
			List<Object> ops = new ArrayList<Object>();
			List<InputPosition> elocs = new ArrayList<InputPosition>();
			ops.add(convertValue(elocs, substs, e2.fn));
			for (Object o : e2.args)
				ops.add(convertValue(elocs, substs, o));
			// TODO: check this doesn't already exist
			Var var = allocateVar();
			HSIEBlock closure = form.closure(var);
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
			closureDepends.put(var, mydeps);
			return new CreationOfVar(var, e2.location, "clos" + var.idx);
		} else if (expr instanceof CastExpr) {
			CastExpr ce = (CastExpr) expr;
			CreationOfVar cv = (CreationOfVar) convertValue(locs, substs, ce.expr);
			HSIEBlock closure = form.getClosure(cv.var);
			closure.downcastType = (Type) ((AbsoluteVar)ce.castTo).defn;
			return cv;
		}
		else {
			System.out.println(expr);
			throw new UtilException("HSIE Cannot handle " + expr + " " + (expr != null? " of type " + expr.getClass() : ""));
		}
	}

	public List<CreationOfVar> closureDependencies(Object ret) {
		if (ret instanceof Var)
			return closureDepends.get(ret);
		else if (ret instanceof CreationOfVar)
			return closureDepends.get(((CreationOfVar)ret).var);
		return null;
	}
}
