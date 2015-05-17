package org.flasck.flas.hsie;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.flasck.flas.vcode.hsieForm.HSIEBlock;
import org.flasck.flas.vcode.hsieForm.HSIEForm.Var;

public class State implements Iterable<Entry<Var,PattExpr>> {
	private final Map<Var, PattExpr> mapping = new HashMap<Var, PattExpr>();
	private final HSIEBlock writeTo;
	
	public State(HSIEBlock b) {
		this.writeTo = b;
	}
	
	// Create a mapping from v -> patt -> substexpr
	public void associate(Var v, Object patt, SubstExpr expr) {
		if (!mapping.containsKey(v))
			mapping.put(v, new PattExpr());
		mapping.get(v).associate(patt, expr);
	}

	public void dump() {
		for (Entry<Var, PattExpr> e : mapping.entrySet()) {
			System.out.println(e.getKey() + " -> ");
			e.getValue().dump();
		}
	}

	@Override
	public Iterator<Entry<Var, PattExpr>> iterator() {
		return mapping.entrySet().iterator();
	}

}
