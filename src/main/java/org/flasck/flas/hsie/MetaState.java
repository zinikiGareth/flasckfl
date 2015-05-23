package org.flasck.flas.hsie;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flasck.flas.parsedForm.ApplyExpr;
import org.flasck.flas.parsedForm.ItemExpr;
import org.flasck.flas.tokenizers.ExprToken;
import org.flasck.flas.vcode.hsieForm.HSIEBlock;
import org.flasck.flas.vcode.hsieForm.HSIEForm;
import org.flasck.flas.vcode.hsieForm.Var;
import org.zinutils.exceptions.UtilException;

public class MetaState {
	public final HSIEForm form;
	final List<State> allStates = new ArrayList<State>();
	private final Map<Var, Map<String, Var>> fieldVars = new HashMap<Var, Map<String, Var>>();
	private final Map<SubstExpr, Object> retValues = new HashMap<SubstExpr, Object>();
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
		form.vars.add(ret);
		return ret;
	}

	public Var varFor(Var from, String field) {
		if (!fieldVars.containsKey(from))
			fieldVars.put(from, new HashMap<String, Var>());
		if (!fieldVars.get(from).containsKey(field))
			fieldVars.get(from).put(field, allocateVar());
		Var ret = fieldVars.get(from).get(field);
		System.out.println("Allocating " + ret + " for " + from + "." + field);
		return ret;
	}

	public Object getValueFor(SubstExpr e) {
		if (!retValues.containsKey(e)) {
			retValues.put(e, convertValue(e.substs, e.expr));
		}
		return retValues.get(e);
	}

	private Object convertValue(Map<String, Var> substs, Object expr) {
		if (expr instanceof ItemExpr) {
			ItemExpr e2 = (ItemExpr) expr;
			if (e2.tok.type == ExprToken.NUMBER)
				return Integer.parseInt(e2.tok.text); // what about floats?
			else if (e2.tok.type == ExprToken.IDENTIFIER || e2.tok.type == ExprToken.SYMBOL || e2.tok.type == ExprToken.PUNC) {
				if (substs.containsKey(e2.tok.text))
					return substs.get(e2.tok.text);
				else {
					// TODO: resolve these to the global scope
					form.dependsOn(e2.tok.text);
					return e2.tok.text;
				}
			} else
				throw new UtilException("Cannot handle " + e2.tok);
		}
		else if (expr instanceof ApplyExpr) {
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
			System.out.println(expr.getClass());
			throw new UtilException("HSIE Cannot handle " + expr.getClass());
		}
	}

	public List<Var> closureDependencies(Object ret) {
		if (!(ret instanceof Var))
			return null;
		return closureDepends.get(ret);
	}
}
