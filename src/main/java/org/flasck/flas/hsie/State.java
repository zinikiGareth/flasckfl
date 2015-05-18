package org.flasck.flas.hsie;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.flasck.flas.vcode.hsieForm.HSIEBlock;
import org.flasck.flas.vcode.hsieForm.HSIEForm.Var;
import org.zinutils.exceptions.UtilException;

public class State implements Iterable<Entry<Var,PattExpr>> {
	private final Map<Var, PattExpr> mapping = new HashMap<Var, PattExpr>();
	public final HSIEBlock writeTo;
	
	public State(HSIEBlock b) {
		this.writeTo = b;
	}
	
	// Create a mapping from v -> patt -> substexpr
	public void associate(Var v, Object patt, SubstExpr expr) {
		if (v == null)
			throw new UtilException("Cannot use null var");
		if (!mapping.containsKey(v))
			mapping.put(v, new PattExpr());
		mapping.get(v).associate(patt, expr);
	}

	public State cloneEliminate(Var var, HSIEBlock into) {
		State ret = new State(into);
		for (Entry<Var, PattExpr> x : mapping.entrySet()) {
			if (x.getKey().equals(var))
				continue;
			ret.mapping.put(x.getKey(), x.getValue().duplicate());
		}
		return ret;
	}
	
	public void eliminate(Var var) {
		mapping.remove(var);
	}

	public boolean hasNeeds() {
		return !mapping.isEmpty();
	}

	public PattExpr get(Var var) {
		return mapping.get(var);
	}

	@Override
	public Iterator<Entry<Var, PattExpr>> iterator() {
		return mapping.entrySet().iterator();
	}

	public void dump() {
		for (Entry<Var, PattExpr> e : mapping.entrySet()) {
			System.out.println(e.getKey() + " -> ");
			e.getValue().dump();
		}
	}
}
