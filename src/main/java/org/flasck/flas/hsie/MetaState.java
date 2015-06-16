package org.flasck.flas.hsie;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flasck.flas.parsedForm.AbsoluteVar;
import org.flasck.flas.parsedForm.ApplyExpr;
import org.flasck.flas.parsedForm.CardFunction;
import org.flasck.flas.parsedForm.CardMember;
import org.flasck.flas.parsedForm.ExternalRef;
import org.flasck.flas.parsedForm.HandlerLambda;
import org.flasck.flas.parsedForm.LocalVar;
import org.flasck.flas.parsedForm.NumericLiteral;
import org.flasck.flas.parsedForm.ObjectReference;
import org.flasck.flas.parsedForm.StringLiteral;
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
		Var ret = new Var(form.vars.size());
//		System.out.println("Allocating var " + ret);
		form.vars.add(ret);
		return ret;
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
		if (expr instanceof ApplyExpr) {
			ApplyExpr ae = (ApplyExpr) expr;
			if (ae.fn instanceof AbsoluteVar) {
				AbsoluteVar fn = (AbsoluteVar)ae.fn;
				if (fn.id.equals("if")) {
					HSIEBlock ifCmd = writeTo.ifCmd((Var) convertValue(substs, ae.args.get(0)));
					writeIfExpr(substs, ae.args.get(1), ifCmd);
					Object orelse = ae.args.size() == 3 ? ae.args.get(2) : null;
					if (orelse != null)
						writeIfExpr(substs, orelse, writeTo);
					else
						writeTo.caseError();
					return;
				} else if (fn.id.equals("let")) {
					substs.put(((LocalVar)ae.args.get(0)).var, (Var)getValueFor(substs, ae.args.get(1)));
					writeIfExpr(substs, ae.args.get(2), writeTo);
					return;
				}
			}
		}
		writeFinalExpr(substs, expr, writeTo);
	}

	public void writeFinalExpr(Map<String, Var> substs, Object expr, HSIEBlock writeTo) {
		Object ret = getValueFor(substs, expr);
		writeTo.doReturn(ret, closureDependencies(ret));
	}

	public Object getValueFor(Map<String, Var> substs, Object e) {
		if (!retValues.containsKey(e)) {
			retValues.put(e, convertValue(substs, e));
		}
		return retValues.get(e);
	}

	private Object convertValue(Map<String, Var> substs, Object expr) {
		if (expr == null) // mainly error trapping, but valid in if .. if .. <no else> case
			return null;
		else if (expr instanceof NumericLiteral)
			return Integer.parseInt(((NumericLiteral)expr).text); // what about floats?
		else if (expr instanceof StringLiteral)
			return expr;
		else if (expr instanceof LocalVar) {
			String var = ((LocalVar)expr).var;
			if (!substs.containsKey(var))
				throw new UtilException("How can this be a local var? " + var + " not in " + substs);
			return substs.get(var);
		} else if (expr instanceof AbsoluteVar) {
			String var = ((AbsoluteVar)expr).id;
			form.dependsOn(expr);
			return expr;
		} else if (expr instanceof ObjectReference || expr instanceof CardFunction) {
			String var = ((ExternalRef)expr).uniqueName();
			form.dependsOn(expr);
			return expr;
		} else if (expr instanceof CardMember) {
			String var = ((CardMember)expr).uniqueName();
			form.dependsOn(expr);
			return expr;
		} else if (expr instanceof HandlerLambda) {
			String var = ((HandlerLambda)expr).uniqueName();
			form.dependsOn(expr);
			return expr;
		} else if (expr instanceof ApplyExpr) {
			ApplyExpr e2 = (ApplyExpr) expr;
			List<Object> ops = new ArrayList<Object>();
			ops.add(convertValue(substs, e2.fn));
			for (Object o : e2.args)
				ops.add(convertValue(substs, o));
			// TODO: check this doesn't already exist
			Var var = allocateVar();
			HSIEBlock closure = form.closure(var);
			List<Var> mydeps = new ArrayList<Var>();
			for (Object o : ops) {
				closure.push(o);
				if (o instanceof Var && closureDepends.containsKey(o)) {
					mydeps.addAll(closureDepends.get(o));
					mydeps.add((Var) o);
				}
			}
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
