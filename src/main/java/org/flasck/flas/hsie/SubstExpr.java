package org.flasck.flas.hsie;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.flasck.flas.vcode.hsieForm.Var;
import org.zinutils.exceptions.UtilException;

public class SubstExpr {
	private static int idx = 0;
	public final Object expr;
	public final Map<String, Var> substs = new HashMap<String, Var>();
	private String me;

	public SubstExpr(Object expr) {
		this.expr = expr;
		this.me = "E" + (idx++);
	}

	public SubstExpr alsoSub(Map<String, Var> map) {
		for (Entry<String, Var> x : map.entrySet())
			subst("_scoped." + x.getKey(), x.getValue());
		return this;
	}

	public SubstExpr subst(String varToSubst, Var var) {
		if (substs.containsKey(varToSubst))
			throw new UtilException("Duplicate var in patterns: " + varToSubst); // TODO: this should be proper error handling
		substs.put(varToSubst, var);
		return this;
	}

	@Override
	public String toString() {
		return me + " " + expr.toString() + substsString();
	}

	private String substsString() {
		StringBuilder ret = new StringBuilder();
		for (Entry<String, Var> e : substs.entrySet()) {
			ret.append(","+e.getKey()+"/"+e.getValue());
		}
		if (ret.length() > 0)
			ret.delete(0,1);
		ret.insert(0, "[");
		ret.append("]");
		return ret.toString();
	}
}
