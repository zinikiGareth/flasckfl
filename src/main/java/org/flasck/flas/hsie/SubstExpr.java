package org.flasck.flas.hsie;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.flasck.flas.vcode.hsieForm.HSIEForm.Var;

public class SubstExpr {
	private final Object expr;
	private final Map<String, Var> substs = new HashMap<String, Var>();

	public SubstExpr(Object expr) {
		this.expr = expr;
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
