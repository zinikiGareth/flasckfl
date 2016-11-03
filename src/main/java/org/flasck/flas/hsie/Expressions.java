package org.flasck.flas.hsie;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.flasck.flas.commonBase.IfExpr;
import org.flasck.flas.commonBase.LocatedObject;
import org.flasck.flas.vcode.hsieForm.ClosureCmd;
import org.flasck.flas.vcode.hsieForm.VarInSource;
import org.flasck.flas.vcode.hsieForm.HSIEBlock;
import org.flasck.flas.vcode.hsieForm.HSIEForm;
import org.flasck.flas.vcode.hsieForm.Var;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zinutils.exceptions.UtilException;

public class Expressions {
	static final Logger logger = LoggerFactory.getLogger("HSIE");

	private final HSIEForm form;
	private final List<Object> exprs = new ArrayList<Object>();
	private final Map<Object, LocatedObject> convertedValues = new HashMap<Object, LocatedObject>();

	public Expressions(HSIEForm form) {
		this.form = form;
	}

	public void addExpr(Object expr) {
		exprs.add(expr);
	}

	public void evalExpr(Map<String, VarInSource> substs, State s, Set<Integer> mycases) {
		Integer e = s.singleExpr(mycases);
//		System.out.println("Have expr " + e);
		if (e != null) {
			writeExpr(substs, s.writeTo, e);
		} else {
			if (s.writeTo instanceof HSIEForm)
				s.writeTo.caseError();
		}
	}

	public void writeExpr(Map<String, VarInSource> substs, HSIEBlock writeTo, int expr) {
		writeIfExpr(substs, exprs.get(expr), writeTo);
	}
	
	private void writeIfExpr(Map<String, VarInSource> substs, Object expr, HSIEBlock writeTo) {
		logger.info("Handling " + form.fnName + "; expr = " + expr + "; substs = " + substs);
		// First handle the explicit "if" cases
		if (expr instanceof IfExpr) {
			IfExpr ae = (IfExpr) expr;
			if (!convertedValues.containsKey(expr))
				throw new UtilException("There is no return value for " + ae.guard);
			LocatedObject lo = convertedValues.get(ae.guard);
			HSIEBlock ifCmd = writeTo.ifCmd(lo.loc, (VarInSource) lo.obj);
			writeIfExpr(substs, ae.ifExpr, ifCmd);
			if (ae.elseExpr != null)
				writeIfExpr(substs, ae.elseExpr, writeTo);
			else
				writeTo.caseError();
			return;
		}
		writeFinalExpr(substs, expr, writeTo);
	}

	public void writeFinalExpr(Map<String, VarInSource> substs, Object expr, HSIEBlock writeTo) {
		if (!convertedValues.containsKey(expr))
			throw new UtilException("There is no return value for " + expr);
		LocatedObject lo = convertedValues.get(expr);
		writeTo.doReturn(lo.loc, lo.obj, closureDependencies(lo.obj));
	}

	public List<VarInSource> closureDependencies(Object var) {
		List<VarInSource> ret = new ArrayList<VarInSource>();
		closeDependencies(ret, var);
		return ret;
	}

	private void closeDependencies(List<VarInSource> ret, Object var) {
		ClosureCmd clos = null;
		if (var instanceof Var)
			clos = form.getClosure((Var)var);
		else if (var instanceof VarInSource)
			clos = form.getClosure(((VarInSource)var).var);

		if (clos == null)
			return;

		for (VarInSource cv : clos.depends)
			if (!ret.contains(cv)) {
				closeDependencies(ret, cv);
				if (ret.contains(cv))
					throw new UtilException("I suspect this is a cycle");
				ret.add(cv);
			}
	}

	public List<Object> exprs() {
		return exprs;
	}

	public void translate(Object e, LocatedObject hsieValue) {
		if (!convertedValues.containsKey(e))
			convertedValues.put(e, hsieValue);
	}
}
