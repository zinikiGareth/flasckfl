package org.flasck.flas.hsie;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flasck.flas.commonBase.IfExpr;
import org.flasck.flas.commonBase.LocatedObject;
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

	public final HSIEForm form;
	private final List<Object> exprs = new ArrayList<Object>();
	final List<State> allStates = new ArrayList<State>();
	private final Map<Var, Map<String, Var>> fieldVars = new HashMap<Var, Map<String, Var>>();
	private final Map<Object, LocatedObject> convertedValues = new HashMap<Object, LocatedObject>();
	public final Map<String, CreationOfVar> substs = new HashMap<String, CreationOfVar>();

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

	public void addExpr(Object expr) {
		exprs.add(expr);
	}

	public void subst(String varToSubst, CreationOfVar var) {
		if (substs.containsKey(varToSubst))
			throw new HSIEException(var.loc, "duplicate var in patterns: " + varToSubst);
		MetaState.logger.info("Defining " + varToSubst + " as " + var);
		substs.put(varToSubst, var);
	}

	public void mapVar(String id, CreationOfVar cov) {
		substs.put(id, cov);
	}

	public void writeExpr(HSIEBlock writeTo, int expr) {
		writeIfExpr(substs, exprs.get(expr), writeTo);
	}
	
	private void writeIfExpr(Map<String, CreationOfVar> substs, Object expr, HSIEBlock writeTo) {
		logger.info("Handling " + form.fnName + "; expr = " + expr + "; substs = " + substs);
		// First handle the explicit "if" cases
		if (expr instanceof IfExpr) {
			IfExpr ae = (IfExpr) expr;
			if (!convertedValues.containsKey(expr))
				throw new UtilException("There is no return value for " + ae.guard);
			LocatedObject lo = convertedValues.get(ae.guard);
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
		if (!convertedValues.containsKey(expr))
			throw new UtilException("There is no return value for " + expr);
		LocatedObject lo = convertedValues.get(expr);
		writeTo.doReturn(lo.loc, lo.obj, closureDependencies(lo.obj));
	}

	public List<CreationOfVar> closureDependencies(Object var) {
		List<CreationOfVar> ret = new ArrayList<CreationOfVar>();
		closeDependencies(ret, var);
		return ret;
	}

	private void closeDependencies(List<CreationOfVar> ret, Object var) {
		ClosureCmd clos = null;
		if (var instanceof Var)
			clos = form.getClosure((Var)var);
		else if (var instanceof CreationOfVar)
			clos = form.getClosure(((CreationOfVar)var).var);

		if (clos == null)
			return;

		for (CreationOfVar cv : clos.depends)
			if (!ret.contains(cv)) {
				closeDependencies(ret, cv);
				if (ret.contains(cv))
					throw new UtilException("I suspect this is a cycle");
				ret.add(cv);
			}
	}

	public Object getSubst(String uniqueName) {
		if (!substs.containsKey(uniqueName))
			throw new UtilException("There is no var for " + uniqueName);
		return substs.get(uniqueName);
	}

	public List<Object> exprs() {
		return exprs;
	}

	public void translate(Object e, LocatedObject hsieValue) {
		if (!convertedValues.containsKey(e))
			convertedValues.put(e, hsieValue);
	}
}
