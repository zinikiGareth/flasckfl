package org.flasck.flas.hsie;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.flasck.flas.vcode.hsieForm.HSIEForm.Var;
import org.zinutils.exceptions.UtilException;

public class SubstExpr {
	private final Object expr;
	private final Map<String, Var> substs = new HashMap<String, Var>();

	public SubstExpr(Object expr) {
		this.expr = expr;
	}

	public SubstExpr cloneWith(String varToSubst, Var var) {
		if (substs.containsKey(varToSubst))
			throw new UtilException("Duplicate var in patterns: " + varToSubst); // TODO: this should be proper error handling
		SubstExpr ret = new SubstExpr(expr);
		ret.substs.putAll(substs);
		ret.substs.put(varToSubst, var);
		return ret;
	}
	
	@Override
	public String toString() {
		return expr.toString() + substsString();
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
