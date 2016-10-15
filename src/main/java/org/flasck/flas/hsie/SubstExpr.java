package org.flasck.flas.hsie;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.flasck.flas.vcode.hsieForm.CreationOfVar;
import org.zinutils.exceptions.UtilException;

public class SubstExpr {
	public final Object expr;
	public final Map<String, CreationOfVar> substs = new HashMap<String, CreationOfVar>();
	private String me;

	public SubstExpr(Object expr, int idx) {
		if (expr == null)
			throw new UtilException("Cannot create subst expr with null");
		this.expr = expr;
		this.me = "E" + idx;
	}

	public SubstExpr alsoSub(Map<String, CreationOfVar> map) {
		for (Entry<String, CreationOfVar> x : map.entrySet())
			subst(x.getKey(), x.getValue());
		return this;
	}

	public SubstExpr subst(String varToSubst, CreationOfVar var) {
		if (substs.containsKey(varToSubst))
			throw new UtilException("Duplicate var in patterns: " + varToSubst); // TODO: this should be proper error handling
		if (varToSubst.contains("loadCroset."))
			throw new UtilException("Need .");
		MetaState.logger.info("Creating " + varToSubst +" as " + var);
		substs.put(varToSubst, var);
		return this;
	}

	@Override
	public String toString() {
		return me + " " + expr.toString() + substsString();
	}

	private String substsString() {
		StringBuilder ret = new StringBuilder();
		for (Entry<String, CreationOfVar> e : substs.entrySet()) {
			ret.append(","+e.getKey()+"/"+e.getValue());
		}
		if (ret.length() > 0)
			ret.delete(0,1);
		ret.insert(0, "[");
		ret.append("]");
		return ret.toString();
	}
}
