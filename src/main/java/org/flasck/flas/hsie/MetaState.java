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
import org.flasck.flas.parsedForm.ExternalRef;
import org.flasck.flas.parsedForm.FunctionLiteral;
import org.flasck.flas.parsedForm.HandlerLambda;
import org.flasck.flas.parsedForm.IfExpr;
import org.flasck.flas.parsedForm.IterVar;
import org.flasck.flas.parsedForm.LetExpr;
import org.flasck.flas.parsedForm.LocalVar;
import org.flasck.flas.parsedForm.NumericLiteral;
import org.flasck.flas.parsedForm.ObjectReference;
import org.flasck.flas.parsedForm.StringLiteral;
import org.flasck.flas.parsedForm.TemplateListVar;
import org.flasck.flas.vcode.hsieForm.HSIEBlock;
import org.flasck.flas.vcode.hsieForm.HSIEForm;
import org.flasck.flas.vcode.hsieForm.Var;
import org.zinutils.exceptions.UtilException;

public class MetaState {
	public final HSIEForm form;
	final List<State> allStates = new ArrayList<State>();
	private final Map<Var, Map<String, Var>> fieldVars = new HashMap<Var, Map<String, Var>>();
	private final Map<Object, Object> retValues = new HashMap<Object, Object>();
	private final Map<Var, List<Var>> closureDepends = new HashMap<Var, List<Var>>();

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
	
	private void writeIfExpr(Map<String, Var> substs, Object expr, HSIEBlock writeTo) {
		if (expr instanceof IfExpr) {
			IfExpr ae = (IfExpr) expr;
			List<InputPosition> elocs = new ArrayList<InputPosition>();
			HSIEBlock ifCmd = writeTo.ifCmd((Var) convertValue(elocs, substs, ae.guard));
			writeIfExpr(substs, ae.ifExpr, ifCmd);
			if (ae.elseExpr != null)
				writeIfExpr(substs, ae.elseExpr, writeTo);
			else
				writeTo.caseError();
			return;
		} else if (expr instanceof LetExpr) {
			LetExpr let = (LetExpr) expr;
			Object val = getValueFor(substs, let.val);
			Var var;
			if (val instanceof Var)
				var = (Var) val;
			else {
				var = allocateVar();
				HSIEBlock closure = form.closure(var);
				closure.push(null, val);
			}
			substs.put(let.var, var);
			writeIfExpr(substs, let.expr, writeTo);
			return;
		}
		writeFinalExpr(substs, expr, writeTo);
	}

	public void writeFinalExpr(Map<String, Var> substs, Object expr, HSIEBlock writeTo) {
		Object ret = getValueFor(substs, expr);
		writeTo.doReturn(null, ret, closureDependencies(ret));
	}

	public Object getValueFor(Map<String, Var> substs, Object e) {
		if (!retValues.containsKey(e)) {
			List<InputPosition> elocs = new ArrayList<InputPosition>();
			retValues.put(e, convertValue(elocs, substs, e));
		}
		return retValues.get(e);
	}

	private Object convertValue(List<InputPosition> locs, Map<String, Var> substs, Object expr) {
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
			locs.add(((AbsoluteVar)expr).location);
			String var = ((AbsoluteVar)expr).id;
			form.dependsOn(expr);
			return expr;
		} else if (expr instanceof ObjectReference || expr instanceof CardFunction) {
			locs.add(((ExternalRef)expr).location());
			String var = ((ExternalRef)expr).uniqueName();
			form.dependsOn(expr);
			return expr;
		} else if (expr instanceof CardMember) {
			locs.add(((ExternalRef)expr).location());
			String var = ((CardMember)expr).uniqueName();
			form.dependsOn(expr);
			return expr;
		} else if (expr instanceof CardStateRef) {
			locs.add(((CardStateRef)expr).location());
			return expr;
		} else if (expr instanceof HandlerLambda) {
			locs.add(((ExternalRef)expr).location());
			String var = ((HandlerLambda)expr).uniqueName();
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
			List<Var> mydeps = new ArrayList<Var>();
			if (ops.size() != elocs.size())
				throw new UtilException("Misplaced location or operation: " +  elocs.size() + " != " + ops.size());
			for (int i=0;i<ops.size();i++) {
				Object o = ops.get(i);
				if (elocs.get(i) == null) {
					System.out.println(e2);
					System.out.println(ops);
					System.out.println(elocs);
					System.out.println("Did not find loc for " + i);
				}
				closure.push(elocs.get(i), o);
				if (o instanceof Var && closureDepends.containsKey(o) && !mydeps.contains(o)) {
					mydeps.addAll(closureDepends.get(o));
					mydeps.add((Var) o);
				}
			}
			locs.add(e2.location);
			closureDepends.put(var, mydeps);
			return var;
		}
		else {
			System.out.println(expr);
			throw new UtilException("HSIE Cannot handle " + expr + " " + (expr != null? " of type " + expr.getClass() : ""));
		}
	}

	public List<Var> closureDependencies(Object ret) {
		if (!(ret instanceof Var))
			return null;
		return closureDepends.get(ret);
	}
}
